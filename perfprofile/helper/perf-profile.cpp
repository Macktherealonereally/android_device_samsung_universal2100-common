/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

// Applies the user's performance profile (persist.sys.perf_profile) to the
// libperfmgr power HAL. Started by init whenever the property changes and
// whenever the power HAL (re)starts; exits when done.
//
// Profiles use the old LineageOS PerformanceManager numbering:
//   0 = power save, 1 = balanced (default), 2 = performance, 3 = efficiency.
//
// Power save and efficiency are long-running power HAL hints whose CPU/GPU caps
// outrank the boost hints in powerhint.json. Performance is a long-running hint
// too (faster GPU ramp-up) and flips vendor.powerhal.profile.performance, which
// enables the extra LAUNCH/INTERACTION boosts and the screen-on GPU floor
// (PROFILE_PERFORMANCE_GPU) in powerhint.json. Balanced turns everything off.

#include <aidl/google/hardware/power/extension/pixel/IPowerExt.h>
#include <android-base/logging.h>
#include <android-base/properties.h>
#include <android/binder_manager.h>

#include <array>
#include <memory>
#include <string>

using ::aidl::google::hardware::power::extension::pixel::IPowerExt;
using ::android::base::GetIntProperty;
using ::android::base::SetProperty;

namespace {

constexpr char kProfileProp[] = "persist.sys.perf_profile";
constexpr char kPerformanceProp[] = "vendor.powerhal.profile.performance";
constexpr char kPowerInstance[] = "android.hardware.power.IPower/default";

constexpr int kPowerSave = 0;
constexpr int kBalanced = 1;
constexpr int kPerformance = 2;
constexpr int kEfficiency = 3;

struct ProfileHint {
    int profile;
    const char* hint;
};

constexpr std::array<ProfileHint, 3> kProfileHints = {{
        {kPowerSave, "PROFILE_POWER_SAVE"},
        {kEfficiency, "PROFILE_EFFICIENCY"},
        {kPerformance, "PROFILE_PERFORMANCE"},
}};

// Started by PROFILE_PERFORMANCE and by screen-on/touch/launch hints while the
// performance property is set; ending PROFILE_PERFORMANCE doesn't end it.
constexpr char kPerformanceGpuHint[] = "PROFILE_PERFORMANCE_GPU";

const char* ProfileName(int profile) {
    switch (profile) {
        case kPowerSave:
            return "power save";
        case kBalanced:
            return "balanced";
        case kPerformance:
            return "performance";
        case kEfficiency:
            return "efficiency";
    }
    return "unknown";
}

int ReadProfile() {
    return GetIntProperty(kProfileProp, kBalanced, kPowerSave, kEfficiency);
}

std::shared_ptr<IPowerExt> ConnectPowerExt() {
    ndk::SpAIBinder power(AServiceManager_waitForService(kPowerInstance));
    if (power.get() == nullptr) {
        LOG(ERROR) << "Cannot get " << kPowerInstance;
        return nullptr;
    }

    ndk::SpAIBinder ext;
    if (AIBinder_getExtension(power.get(), ext.getR()) != STATUS_OK || ext.get() == nullptr) {
        LOG(ERROR) << "Power HAL has no extension";
        return nullptr;
    }

    return IPowerExt::fromBinder(ext);
}

bool Apply(const std::shared_ptr<IPowerExt>& ext, int profile) {
    bool ok = true;

    // Boosts first, so that leaving performance never runs a stray boost.
    if (!SetProperty(kPerformanceProp, profile == kPerformance ? "true" : "false")) {
        LOG(ERROR) << "Failed to set " << kPerformanceProp;
        ok = false;
    }

    // Start the new profile's hint before ending the old one: the caps in
    // powerhint.json are ordered by priority, so switching between two capped
    // profiles never lifts the caps in between.
    for (const auto& p : kProfileHints) {
        if (p.profile != profile) continue;
        bool supported = false;
        if (!ext->isModeSupported(p.hint, &supported).isOk() || !supported) {
            LOG(ERROR) << p.hint << " is not in powerhint.json";
            ok = false;
            continue;
        }
        ok = ext->setMode(p.hint, true).isOk() && ok;
    }
    for (const auto& p : kProfileHints) {
        if (p.profile == profile) continue;
        ok = ext->setMode(p.hint, false).isOk() && ok;
    }
    if (profile != kPerformance) {
        ok = ext->setMode(kPerformanceGpuHint, false).isOk() && ok;
    }

    return ok;
}

}  // namespace

int main(int /* argc */, char** argv) {
    android::base::InitLogging(argv, android::base::LogdLogger(android::base::SYSTEM));

    std::shared_ptr<IPowerExt> ext = ConnectPowerExt();
    if (ext == nullptr) {
        return 1;
    }

    // init does not restart a oneshot service that is still running, so pick up
    // any change that arrived while we were applying the previous one.
    int profile = ReadProfile();
    for (int i = 0; i < 10; i++) {
        bool ok = Apply(ext, profile);
        LOG(INFO) << "Performance profile: " << ProfileName(profile) << (ok ? "" : " (with errors)");

        int now = ReadProfile();
        if (now == profile) {
            return ok ? 0 : 1;
        }
        profile = now;
    }

    return 1;
}
