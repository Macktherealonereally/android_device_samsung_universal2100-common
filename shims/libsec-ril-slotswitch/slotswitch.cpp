//
// SPDX-FileCopyrightText: The LineageOS Project
// SPDX-License-Identifier: Apache-2.0
//

// Restores the Galaxy S21 ("tsds2") SIM2 <-> eSIM slot switch in the
// A556 RIL we ship.
//
// On tsds2 devices the second SIM tray and the embedded eUICC share one
// SIM interface. One UI (TelephonyUI SlotMappingManager) switches between
// them with the plain AOSP API:
//
//   eSIM: setSimSlotMapping([{port 0, phys 0, log 0}, {port 0, phys 2, log 1}])
//   pSIM: setSimSlotMapping([{port 0, phys 0, log 0}, {port 0, phys 1, log 1}])
//
// The stock S21 libsec-ril (G991BXXSJHZC2) catches these two requests in
// SimManager::DoSetSlotMapping, stores the target (1 = pSIM, 2 = eSIM) at
// SimManager+0x1668 and starts SimManager::ExecuteSlotSwitch(req, 0x21).
// OnSimLowLevelControlDone then walks the modem through
// 0x21 -> 0x10 (eSIM) / 0x11 (pSIM) -> 0x20 and answers the original
// request. The A556 RIL still contains ExecuteSlotSwitch and the whole
// OnSimLowLevelControlDone state machine, but its DoSetSlotMapping only
// knows the A35/A55 MEP layout and forwards physical slot 2 to the modem as
// a normal slot mapping, which the S21 modem rejects (INTERNAL_ERR).
//
// This library is linked with -z global (DF_1_GLOBAL) and loaded as a
// DT_NEEDED of the libsec-ril shim, i.e. before libsec-ril-impl.so is
// dlopen()ed. The impl calls SimManager::DoSetSlotMapping through its PLT,
// so the definition below takes precedence; everything that is not a tsds2
// switch request is handed to the real implementation unchanged.

#include <dlfcn.h>
#include <stddef.h>
#include <sys/system_properties.h>

#include <stdint.h>
#include <stdlib.h>
#include <string.h>

#include <log/log.h>

#undef LOG_TAG
#define LOG_TAG "sec-ril-slotswitch"

namespace {

constexpr char kRealLib[] = "/vendor/lib64/libsec-ril-impl.so";
constexpr char kDoSetSlotMapping[] = "_ZN10SimManager16DoSetSlotMappingEP7Request";
constexpr char kExecuteSlotSwitch[] = "_ZN10SimManager17ExecuteSlotSwitchEP7Requesti";

// Offsets taken from both RILs (G991B HZC2 stock and A556EXXSABYI4).
constexpr size_t kRequestDataOffset = 0x38;      // Request::mData
constexpr size_t kSlotMapCountOffset = 0x0c;     // slot map: int count
constexpr size_t kSlotMapEntriesOffset = 0x10;   // slot map: {int phy, int port}[]
constexpr size_t kSimManagerSlotOffset = 0x14;   // SimManager: RIL socket/slot id
constexpr size_t kSimManagerTargetOffset = 0x1668;  // SimManager: switch target

constexpr int kTargetPsim = 1;
constexpr int kTargetEsim = 2;
constexpr int kActionStartSwitch = 0x21;

using DoSetSlotMappingFn = int (*)(void*, void*);
using ExecuteSlotSwitchFn = void (*)(void*, void*, int);

DoSetSlotMappingFn gRealDoSetSlotMapping = nullptr;
ExecuteSlotSwitchFn gExecuteSlotSwitch = nullptr;

bool resolveReal() {
    if (gRealDoSetSlotMapping != nullptr && gExecuteSlotSwitch != nullptr) {
        return true;
    }

    // The impl is already loaded (it is the caller). dlsym() on its handle
    // searches the impl first, so this returns its own definitions and not
    // the interposer below.
    void* handle = dlopen(kRealLib, RTLD_NOW | RTLD_NOLOAD);
    if (handle == nullptr) {
        ALOGE("dlopen(%s, RTLD_NOLOAD) failed: %s", kRealLib, dlerror());
        return false;
    }

    gRealDoSetSlotMapping =
            reinterpret_cast<DoSetSlotMappingFn>(dlsym(handle, kDoSetSlotMapping));
    gExecuteSlotSwitch = reinterpret_cast<ExecuteSlotSwitchFn>(dlsym(handle, kExecuteSlotSwitch));
    if (gRealDoSetSlotMapping == nullptr || gExecuteSlotSwitch == nullptr) {
        ALOGE("dlsym failed (DoSetSlotMapping=%p ExecuteSlotSwitch=%p)",
              reinterpret_cast<void*>(gRealDoSetSlotMapping),
              reinterpret_cast<void*>(gExecuteSlotSwitch));
        return false;
    }

    return true;
}

int getPropInt(const char* name, int def) {
    char value[PROP_VALUE_MAX] = {};
    if (__system_property_get(name, value) <= 0) {
        return def;
    }
    char* end = nullptr;
    long parsed = strtol(value, &end, 10);
    return end == value ? def : static_cast<int>(parsed);
}

bool isTsds2() {
    char value[PROP_VALUE_MAX] = {};
    if (__system_property_get("persist.radio.esim.slotswitch", value) <= 0) {
        return false;
    }
    return strcmp(value, "tsds2") == 0;
}

template <typename T>
T* at(void* base, size_t offset) {
    return reinterpret_cast<T*>(static_cast<uint8_t*>(base) + offset);
}

// Returns kTargetPsim/kTargetEsim when the request is a One UI style tsds2
// switch request, 0 otherwise. Mirrors the stock G991B logic.
int getSwitchTarget(void* slotMap) {
    const int count = *at<int>(slotMap, kSlotMapCountOffset);
    const int* map = at<int>(slotMap, kSlotMapEntriesOffset);

    ALOGI("DoSetSlotMapping: count %d", count);
    for (int i = 0; i < count && i < 4; i++) {
        ALOGI("  logical %d -> phy %d port %d", i, map[2 * i], map[2 * i + 1]);
    }

    if (count != 2 || map[0] != 0 || map[1] != 0 || map[3] != 0) {
        return 0;
    }

    const bool esimActive =
            getPropInt("ril.simslottype1", 0) == 1 || getPropInt("ril.simslottype2", 0) == 1;
    if (!esimActive && map[2] == 2) {
        return kTargetEsim;
    }
    if (esimActive && map[2] == 1) {
        return kTargetPsim;
    }
    return 0;
}

}  // namespace

// int SimManager::DoSetSlotMapping(Request*)
extern "C" __attribute__((visibility("default"))) int _ZN10SimManager16DoSetSlotMappingEP7Request(
        void* self, void* request) {
    if (!resolveReal()) {
        abort();  // Cannot forward; rild restarts and the interposer is logged.
    }

    void* slotMap = request != nullptr ? *at<void*>(request, kRequestDataOffset) : nullptr;
    if (slotMap == nullptr || !isTsds2()) {
        return gRealDoSetSlotMapping(self, request);
    }

    const int target = getSwitchTarget(slotMap);
    if (target == 0) {
        return gRealDoSetSlotMapping(self, request);
    }

    ALOGI("tsds2 slot switch to %s (SimManager slot %d, simslottype %d/%d)",
          target == kTargetEsim ? "eSIM" : "pSIM", *at<int>(self, kSimManagerSlotOffset),
          getPropInt("ril.simslottype1", 0), getPropInt("ril.simslottype2", 0));

    // Same as stock: remember the target, start the low-level control
    // sequence; OnSimLowLevelControlDone answers the request when done.
    *at<int>(self, kSimManagerTargetOffset) = target;
    gExecuteSlotSwitch(self, request, kActionStartSwitch);
    return 0;
}
