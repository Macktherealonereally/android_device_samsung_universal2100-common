#
# Copyright (C) 2025 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

COMMON_PATH := device/samsung/universal2100-common

## Architecture
TARGET_ARCH := arm64
TARGET_ARCH_VARIANT := armv8-2a-dotprod
TARGET_CPU_ABI := arm64-v8a
TARGET_CPU_ABI2 :=
TARGET_CPU_VARIANT := cortex-a76

TARGET_2ND_ARCH := arm
TARGET_2ND_ARCH_VARIANT := armv8-a
TARGET_2ND_CPU_ABI := armeabi-v7a
TARGET_2ND_CPU_ABI2 := armeabi
TARGET_2ND_CPU_VARIANT := cortex-a76

## Bluetooth
BOARD_HAVE_BLUETOOTH := true
BOARD_HAVE_BLUETOOTH_BCM := true
BOARD_CUSTOM_BT_CONFIG := $(COMMON_PATH)/bluetooth/libbt_vndcfg.txt

## Display
BOARD_MINIMUM_DISPLAY_BRIGHTNESS := 1

## Platform
BOARD_VENDOR := samsung
TARGET_BOARD_PLATFORM := universal2100
TARGET_BOOTLOADER_BOARD_NAME := exynos2100
TARGET_SOC := exynos2100

include hardware/samsung_slsi-linaro/config/BoardConfig2100.mk

## Kernel source
TARGET_LINUX_KERNEL_VERSION := 5.4
TARGET_KERNEL_SOURCE := kernel/samsung/universal2100
TARGET_KERNEL_CONFIG := \
    gki_defconfig \
    samsung/exynos2100.config \
    samsung/$(TARGET_DEVICE).config

TARGET_KERNEL_NO_GCC := true

## DTB & DTBO
BOARD_DTB_CFG := $(COMMON_PATH)/configs/kernel/$(TARGET_SOC).cfg
BOARD_DTBO_CFG := $(COMMON_PATH)/configs/kernel/$(TARGET_DEVICE).cfg

## Boot Image
BOARD_BOOT_HEADER_VERSION := 3
BOARD_CUSTOM_BOOTIMG := true
BOARD_INCLUDE_DTB_IN_BOOTIMG := true
BOARD_KERNEL_SEPARATED_DTBO := true

BOARD_KERNEL_IMAGE_NAME := Image
BOARD_KERNEL_CMDLINE := loop.max_part=7
BOARD_DTB_OFFSET := 0x0A000000
BOARD_KERNEL_BASE := 0x80000000
BOARD_KERNEL_OFFSET := 0x00080000
BOARD_KERNEL_PAGESIZE := 4096
BOARD_RAMDISK_OFFSET := 0x04000000
BOARD_TAGS_OFFSET := 0x00000000

BOARD_RECOVERY_HEADER_VERSION := 2

BOARD_COMMON_MKBOOTIMG_ARGS := \
    --base $(BOARD_KERNEL_BASE) \
    --ramdisk_offset $(BOARD_RAMDISK_OFFSET) \
    --pagesize $(BOARD_KERNEL_PAGESIZE) \
    --dtb_offset $(BOARD_DTB_OFFSET) \
    --kernel_offset $(BOARD_KERNEL_OFFSET) \
    --tags_offset $(BOARD_TAGS_OFFSET) \
    --board "SRPUG16A018KU" \
    --cmdline "$(BOARD_KERNEL_CMDLINE)"

BOARD_MKBOOTIMG_ARGS := \
    $(BOARD_COMMON_MKBOOTIMG_ARGS) \
    --header_version $(BOARD_BOOT_HEADER_VERSION)

BOARD_RECOVERY_MKBOOTIMG_ARGS := \
    $(BOARD_COMMON_MKBOOTIMG_ARGS) \
    --header_version $(BOARD_RECOVERY_HEADER_VERSION)

## Dynamic Partitions
BOARD_SUPER_PARTITION_SIZE := 11429478400
BOARD_SUPER_PARTITION_GROUPS := samsung_dynamic_partitions
BOARD_SAMSUNG_DYNAMIC_PARTITIONS_SIZE := 11425284096
BOARD_SAMSUNG_DYNAMIC_PARTITIONS_PARTITION_LIST := system system_ext vendor product odm vendor_dlkm

-include vendor/lineage/config/BoardConfigReservedSize.mk

## Filesystem
BOARD_CACHEIMAGE_FILE_SYSTEM_TYPE := ext4
BOARD_ODMIMAGE_FILE_SYSTEM_TYPE := ext4
BOARD_PRODUCTIMAGE_FILE_SYSTEM_TYPE := ext4
BOARD_SYSTEMIMAGE_FILE_SYSTEM_TYPE := erofs
BOARD_SYSTEM_EXTIMAGE_FILE_SYSTEM_TYPE := ext4
BOARD_VENDORIMAGE_FILE_SYSTEM_TYPE := erofs
BOARD_VENDOR_DLKMIMAGE_FILE_SYSTEM_TYPE := erofs

TARGET_COPY_OUT_ODM := odm
TARGET_COPY_OUT_PRODUCT := product
TARGET_COPY_OUT_SYSTEM_EXT := system_ext
TARGET_COPY_OUT_VENDOR := vendor
TARGET_COPY_OUT_VENDOR_DLKM := vendor_dlkm

## Partitions
BOARD_FLASH_BLOCK_SIZE := 4096
BOARD_CACHEIMAGE_PARTITION_SIZE := 629145600
BOARD_DTBOIMG_PARTITION_SIZE := 8388608
BOARD_BOOTIMAGE_PARTITION_SIZE := 67108864
BOARD_VENDOR_BOOTIMAGE_PARTITION_SIZE := $(BOARD_BOOTIMAGE_PARTITION_SIZE)
BOARD_RECOVERYIMAGE_PARTITION_SIZE := $(BOARD_BOOTIMAGE_PARTITION_SIZE)

## Metadata
BOARD_USES_METADATA_PARTITION := true

BOARD_ROOT_EXTRA_FOLDERS := efs

## Manifest
# HIDL
DEVICE_FRAMEWORK_COMPATIBILITY_MATRIX_FILE += \
    $(COMMON_PATH)/device_framework_matrix.xml \
    hardware/samsung/vintf/samsung_framework_compatibility_matrix.xml

DEVICE_MANIFEST_FILE += $(COMMON_PATH)/manifest.xml
DEVICE_MATRIX_FILE += $(COMMON_PATH)/compatibility_matrix.xml

## Properties
TARGET_PRODUCT_PROP += $(COMMON_PATH)/product.prop
TARGET_SYSTEM_PROP += $(COMMON_PATH)/system.prop
TARGET_VENDOR_PROP += $(COMMON_PATH)/vendor.prop

## Recovery
BOARD_INCLUDE_RECOVERY_DTBO := true
TARGET_RECOVERY_FSTAB := $(COMMON_PATH)/init/fstab.exynos2100
TARGET_RECOVERY_PIXEL_FORMAT := ABGR_8888
TARGET_USERIMAGES_USE_EXT4 := true
TARGET_USERIMAGES_USE_F2FS := true

## Releasetools
TARGET_RELEASETOOLS_EXTENSIONS := $(COMMON_PATH)/releasetools

## Security
VENDOR_SECURITY_PATCH := 2026-01-01

## RIL
ENABLE_VENDOR_RIL_SERVICE := true

## SECCOMP Policy
BOARD_SECCOMP_POLICY += $(COMMON_PATH)/seccomp

## SELinux
BOARD_SEPOLICY_TEE_FLAVOR := teegris
include device/samsung_slsi/sepolicy/sepolicy.mk
include device/lineage/sepolicy/exynos/sepolicy.mk

BOARD_VENDOR_SEPOLICY_DIRS += $(COMMON_PATH)/sepolicy/vendor
SYSTEM_EXT_PRIVATE_SEPOLICY_DIRS += $(COMMON_PATH)/sepolicy/private

# IMS (ImsStack/ImsMedia) - see ims/ims.mk
ifneq ($(wildcard packages/modules/ImsMedia/sepolicy/system_ext/private/imsmedia.te),)
SYSTEM_EXT_PRIVATE_SEPOLICY_DIRS += \
    packages/modules/ImsMedia/sepolicy/system_ext/private \
    $(COMMON_PATH)/ims/sepolicy/system_ext/private
endif

## Verified Boot
BOARD_AVB_ENABLE := true
BOARD_AVB_MAKE_VBMETA_IMAGE_ARGS += --flags 3
BOARD_AVB_ROLLBACK_INDEX := $(PLATFORM_SECURITY_PATCH_TIMESTAMP)
BOARD_AVB_RECOVERY_ALGORITHM := SHA256_RSA4096
BOARD_AVB_RECOVERY_KEY_PATH := external/avb/test/data/testkey_rsa4096.pem
BOARD_AVB_RECOVERY_ROLLBACK_INDEX := 1
BOARD_AVB_RECOVERY_ROLLBACK_INDEX_LOCATION := 1

## Wifi related defines
BOARD_WLAN_DEVICE                             := bcmdhd
BOARD_WPA_SUPPLICANT_DRIVER                   := NL80211
BOARD_WPA_SUPPLICANT_PRIVATE_LIB              := lib_driver_cmd_bcmdhd
BOARD_HOSTAPD_DRIVER                          := NL80211
BOARD_HOSTAPD_PRIVATE_LIB                     := lib_driver_cmd_bcmdhd
WIFI_AVOID_IFACE_RESET_MAC_CHANGE             := true
WIFI_FEATURE_HOSTAPD_11AX                     := true
WIFI_HIDL_FEATURE_DUAL_INTERFACE              := true
WIFI_HIDL_UNIFIED_SUPPLICANT_SERVICE_RC_ENTRY := true
WPA_SUPPLICANT_VERSION                        := VER_0_8_X

$(call soong_config_set_bool,wpa_supplicant_8,board_wlan_bcmdhd_sae,true)

## Inherit proprietary vendor configuration
include vendor/samsung/universal2100-common/BoardConfigVendor.mk

## Inherit Samsung SLSI board common configuration
include hardware/samsung_slsi-linaro/config/BoardConfigCommon.mk
