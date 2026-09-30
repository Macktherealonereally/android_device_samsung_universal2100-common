#
# Copyright (C) 2025 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

COMMON_PATH := device/samsung/universal2100-common

# Inherit non-AB Device
$(call inherit-product, $(SRC_TARGET_DIR)/product/non_ab_device.mk)

# Setup dalvik vm configs
$(call inherit-product, frameworks/native/build/phone-xhdpi-6144-dalvik-heap.mk)

# Enable project quotas and casefolding for emulated storage without sdcardfs
$(call inherit-product, $(SRC_TARGET_DIR)/product/emulated_storage.mk)

# Inherit Samsung SLSI build config
$(call inherit-product, hardware/samsung_slsi-linaro/config/config.mk)

# Inherit from the proprietary version
$(call inherit-product, vendor/samsung/universal2100-common/universal2100-common-vendor.mk)

# API
PRODUCT_SHIPPING_API_LEVEL := 30
BOARD_SHIPPING_API_LEVEL := 30

# Partitions
PRODUCT_USE_DYNAMIC_PARTITIONS := true

# Audio
PRODUCT_PACKAGES += \
    android.hardware.audio@7.0-impl:32 \
    android.hardware.audio.service \
    android.hardware.audio.effect@7.0-impl:32 \
    android.hardware.soundtrigger@2.3-impl:32 \
    audio.bluetooth.default \
    android.hardware.bluetooth.audio-impl \
    audio.r_submix.default \
    audio.usbv2.default

TARGET_EXCLUDES_AUDIOFX := true

PRODUCT_PACKAGES += \
    SamsungDAP

PRODUCT_PACKAGES += \
    audio_effects.exynos2100 \
    audio_policy_config.exynos2100

PRODUCT_PACKAGES += \
    sound_trigger.primary.exynos2100

PRODUCT_COPY_FILES += \
    frameworks/av/services/audiopolicy/config/audio_policy_volumes.xml:$(TARGET_COPY_OUT_VENDOR)/etc/audio_policy_volumes.xml \
    frameworks/av/services/audiopolicy/config/bluetooth_with_le_audio_policy_configuration_7_0.xml:$(TARGET_COPY_OUT_VENDOR)/etc/bluetooth_with_le_audio_policy_configuration_7_0.xml \
    frameworks/av/services/audiopolicy/config/default_volume_tables.xml:$(TARGET_COPY_OUT_VENDOR)/etc/default_volume_tables.xml \
    frameworks/av/services/audiopolicy/config/r_submix_audio_policy_configuration.xml:$(TARGET_COPY_OUT_VENDOR)/etc/r_submix_audio_policy_configuration.xml

PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.audio.low_latency.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.audio.low_latency.xml \
    frameworks/native/data/etc/android.hardware.audio.pro.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.audio.pro.xml \
    frameworks/native/data/etc/android.software.midi.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.software.midi.xml

# Bluetooth
PRODUCT_PACKAGES += \
    android.hardware.bluetooth@1.0-impl:64 \
    android.hardware.bluetooth@1.0-service \
    libbt-vendor:64

PRODUCT_PACKAGES += \
    android.hardware.bluetooth.prebuilt.xml \
    android.hardware.bluetooth_le.prebuilt.xml

$(call soong_config_set,brcm_libbt,custom_bt_config,//$(COMMON_PATH):exynos2100.txt)

# Charger
PRODUCT_PACKAGES += \
    charger_res_images_vendor

# Camera
PRODUCT_PACKAGES += \
    android.hardware.camera.provider-service.samsung \
    libsensorndkbridge \
    libhwjpeg

$(call soong_config_set_bool,samsungCameraVars,usage_64bit,true)
$(call soong_config_set_bool,samsungCameraVars,high_fps_video,true)

PRODUCT_PACKAGES += \
    android.hardware.camera.concurrent.prebuilt.xml \
    android.hardware.camera.flash-autofocus.prebuilt.xml \
    android.hardware.camera.front.prebuilt.xml \
    android.hardware.camera.full.prebuilt.xml \
    android.hardware.camera.raw.prebuilt.xml

PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.camera.ar.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.camera.ar.xml \
    frameworks/native/data/etc/android.hardware.camera.autofocus.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.camera.autofocus.xml \
    frameworks/native/data/etc/android.hardware.camera.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.camera.xml

# Codec2
PRODUCT_PACKAGES += \
    samsung.hardware.media.c2@1.2-service \
    libExynosC2H264Dec \
    libExynosC2H264Enc \
    libExynosC2HevcDec \
    libExynosC2HevcEnc \
    libExynosC2Vp8Dec \
    libExynosC2Vp8Enc \
    libExynosC2Vp9Dec \
    libExynosC2Vp9Enc \
    libExynosC2Av1Dec

# Media
PRODUCT_PACKAGES += \
    media_codecs_c2.exynos2100 \
    media_codecs_performance_c2.exynos2100 \
    media_profiles_V1_0.exynos2100

# ConfigStore
PRODUCT_PACKAGES += \
    disable_configstore

# Display
PRODUCT_PACKAGES += \
    AdvancedDisplay

# DRM
PRODUCT_PACKAGES += \
    android.hardware.drm-service.clearkey

# DOZE
PRODUCT_PACKAGES += \
    SamsungDoze

# EPIC
PRODUCT_PACKAGES += \
    libepicoperator \
    vendor.samsung_slsi.hardware.epic-service

# fastbootd
PRODUCT_PACKAGES += \
    fastbootd

# Fingerprint
PRODUCT_PACKAGES += \
    android.hardware.biometrics.fingerprint-service.samsung \
    SamsungBiometrics

PRODUCT_PACKAGES += \
    android.hardware.fingerprint.prebuilt.xml

# GPS
PRODUCT_PACKAGES += \
    android.hardware.location.gps.prebuilt.xml

# Graphics
PRODUCT_PACKAGES += \
    android.hardware.graphics.mapper@4.0-impl \
    android.hardware.graphics.allocator@4.0-service \
    android.hardware.graphics.allocator@4.0-impl

PRODUCT_PACKAGES += \
    android.hardware.composer.hwc3-service.slsi \
    libexynosgraphicbuffer_public \
    libion_exynos \
    libion

PRODUCT_PACKAGES += \
    vendor.samsung_slsi.hardware.SbwcDecompService@1.0-service

PRODUCT_PACKAGES += \
    android.hardware.vulkan.compute-0.prebuilt.xml \
    android.hardware.vulkan.level-1.prebuilt.xml

PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.opengles.aep.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.opengles.aep.xml \
    frameworks/native/data/etc/android.software.opengles.deqp.level-2020-03-01.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.software.opengles.deqp.level.xml \
    frameworks/native/data/etc/android.hardware.vulkan.version-1_1.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.vulkan.version.xml \
    frameworks/native/data/etc/android.software.vulkan.deqp.level-2020-03-01.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.software.vulkan.deqp.level.xml

# Overlays
DEVICE_PACKAGE_OVERLAYS += $(COMMON_PATH)/overlay
PRODUCT_ENFORCE_RRO_TARGETS := *

# Gatekeeper
PRODUCT_PACKAGES += \
    android.hardware.gatekeeper@1.0-impl:64 \
    android.hardware.gatekeeper@1.0-service

# Health
PRODUCT_PACKAGES += \
    android.hardware.health-service.samsung \
    android.hardware.health-service.samsung-recovery

# Lineage Health
PRODUCT_PACKAGES += \
    vendor.lineage.health-service.samsung

# sec_battery turns the buck off when charging is disabled above store_mode_charging_max
# (or always with battery,store_mode_buckoff), so the device runs from the battery.
$(call soong_config_set_bool,samsungHealthVars,charging_control_supports_bypass,false)
# batt_full_capacity keeps the device on charger power at the limit
$(call soong_config_set_bool,samsungHealthVars,charging_control_supports_limit,true)

# Keymaster
PRODUCT_PACKAGES += \
    android.hardware.keymaster@4.0-service.samsung \
    libkeymaster4_1support.vendor:64

$(call soong_config_set,samsungVars,target_keymaster4_library,//vendor/samsung/universal2100-common:libskeymaster4device)

# Kernel
PRODUCT_OTA_ENFORCE_VINTF_KERNEL_REQUIREMENTS := true
PRODUCT_ENABLE_UFFD_GC := true
PRODUCT_SET_DEBUGFS_RESTRICTIONS := true

# init
PRODUCT_PACKAGES += \
    init.exynos2100 \
    init.samsung \
    init.usb.exynos2100 \
    init.ueventd.exynos2100 \
    init.recovery.exynos2100 \
    init.epic.exynos2100 \
    init.gps.exynos2100 \
    init.fingerprint.exynos2100 \
    init.nfc.exynos2100 \
    fstab.exynos2100.etc \
    fstab.exynos2100.vendor_ramdisk \
    fstab.exynos2100.ramdisk

PRODUCT_COPY_FILES += \
    $(COMMON_PATH)/init/fstab.exynos2100:$(TARGET_COPY_OUT_VENDOR_RAMDISK)/first_stage_ramdisk/fstab.exynos2100

# Neural Networks
PRODUCT_PACKAGES += \
    android.hardware.neuralnetworks@1.3-service.eden-drv \
    vendor.samsung_slsi.hardware.eden_runtime@1.0-service

# Public Libraries
PRODUCT_PACKAGES += \
    public.libraries.exynos2100

# Soong Namespaces
PRODUCT_SOONG_NAMESPACES += \
    $(COMMON_PATH) \
    hardware/broadcom/libbt \
    hardware/google/interfaces \
    hardware/google/pixel \
    hardware/samsung \
    hardware/samsung_slsi-linaro/exynos/cpboot_v3 \
    hardware/samsung_slsi-linaro/exynos/epic \
    hardware/samsung_slsi-linaro/exynos/gralloc/gralloc4

# Sensors
PRODUCT_PACKAGES += \
    android.hardware.sensors-service.samsung-multihal \
    sensors.sensorhub_wait_for_mcu

PRODUCT_PACKAGES += \
    android.hardware.sensor.accelerometer.prebuilt.xml \
    android.hardware.sensor.barometer.prebuilt.xml \
    android.hardware.sensor.compass.prebuilt.xml \
    android.hardware.sensor.gyroscope.prebuilt.xml \
    android.hardware.sensor.hifi_sensors.prebuilt.xml \
    android.hardware.sensor.light.prebuilt.xml \
    android.hardware.sensor.proximity.prebuilt.xml \
    android.hardware.sensor.stepcounter.prebuilt.xml \
    android.hardware.sensor.stepdetector.prebuilt.xml

# NFC
PRODUCT_PACKAGES += \
    android.hardware.nfc-service.nxp

PRODUCT_PACKAGES += \
    com.android.nfc_extras \
    Tag

PRODUCT_PACKAGES += \
    libnfc-nci.exynos2100 \
    libnfc-nxp.exynos2100 \
    libese-nxp.exynos2100

PRODUCT_PACKAGES += \
    android.hardware.nfc.hcef.prebuilt.xml \
    android.hardware.nfc.ese.prebuilt.xml \
    android.hardware.nfc.hce.prebuilt.xml \
    android.hardware.nfc.prebuilt.xml \
    android.hardware.se.omapi.uicc.prebuilt.xml \
    android.hardware.se.omapi.ese.prebuilt.xml \
    com.nxp.mifare.prebuilt.xml

# Secure Element
PRODUCT_PACKAGES += android.hardware.secure_element-service.nxp

# Memtrack
PRODUCT_PACKAGES += \
    android.hardware.memtrack-service.samsung-mali

# Power
PRODUCT_PACKAGES += \
    android.hardware.power-service.pixel-libperfmgr

PRODUCT_PACKAGES += \
    powerhint.json.exynos2100 \
    task_profiles.json.exynos2100

# PowerShare
PRODUCT_PACKAGES += \
    vendor.lineage.powershare-service.samsung

# RIL
PRODUCT_PACKAGES += \
    cbd \
    libsec-ril \
    secril_config_svc \
    sehradiomanager.conf.exynos2100 \
    sehradiomanager

$(call soong_config_set,cbd,protocol,sipc)

PRODUCT_PACKAGES += \
    android.hardware.telephony.gsm.prebuilt.xml \
    android.hardware.telephony.ims.prebuilt.xml

PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.telephony.satellite.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.telephony.satellite.xml

# IMS (VoLTE/VoWiFi) - only active when volte.xml is synced
$(call inherit-product, $(COMMON_PATH)/ims/ims.mk)

# ipsec_tunnels feature
PRODUCT_PACKAGES += \
    android.software.ipsec_tunnels.prebuilt.xml

# Update
AB_OTA_UPDATER := false
PRODUCT_SOONG_NAMESPACES += bootable/deprecated-ota

# Touch features
PRODUCT_PACKAGES += \
    vendor.lineage.touch-service.samsung

PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.touchscreen.multitouch.jazzhand.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.touchscreen.multitouch.jazzhand.xml

# Thermal
PRODUCT_PACKAGES += \
    android.hardware.thermal-service.pixel \
    thermal_symlinks

PRODUCT_PACKAGES += \
    thermal_info_config.exynos2100

# USB
PRODUCT_PACKAGES += \
    android.hardware.usb-service.samsung \
    android.hardware.usb.gadget-service.samsung

$(call soong_config_set,samsungUsbGadgetVars,gadget_name,10e00000.usb)

PRODUCT_PACKAGES += \
    android.hardware.usb.accessory.prebuilt.xml \
    android.hardware.usb.host.prebuilt.xml

# Vendor service manager
PRODUCT_PACKAGES += \
    vndservicemanager

# Vibrator
PRODUCT_PACKAGES += \
    android.hardware.vibrator-service.samsung

# Weaver
PRODUCT_PACKAGES += \
    android.hardware.weaver@1.0.vendor:64

# WiFi
PRODUCT_PACKAGES += \
    android.hardware.wifi-service \
    hostapd \
    NcmTetheringOverlay \
    WifiOverlay \
    wpa_supplicant \
    wpa_supplicant.conf

PRODUCT_PACKAGES += \
    p2p_supplicant_overlay.conf.exynos2100 \
    wpa_supplicant_overlay.conf.exynos2100

PRODUCT_PACKAGES += \
    android.hardware.wifi.direct.prebuilt.xml \
    android.hardware.wifi.passpoint.prebuilt.xml \
    android.hardware.wifi.prebuilt.xml

PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.wifi.aware.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.wifi.aware.xml \
    frameworks/native/data/etc/android.hardware.wifi.rtt.xml:$(TARGET_COPY_OUT_VENDOR)/etc/permissions/android.hardware.wifi.rtt.xml
