#
# SPDX-FileCopyrightText: The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#
# Open-source IMS (VoLTE/VoWiFi) for lineage-23.2, using krazey's
# Android 17 ImsStack/ImsMedia/CarrierSettings forks in Android 16
# compatibility mode. Mirrors hardware/lineage/generic-ims (lineage-24.0);
# on 24.0 replace this file with
#   $(call inherit-product, hardware/lineage/generic-ims/ims.mk)
#
# Only active when the forks are synced (local manifest volte.xml), so the
# tree still builds without them. AOSP's own ImsMedia has no sepolicy/ dir,
# which is how the krazey ImsMedia is told apart.

ifneq ($(and $(wildcard packages/modules/ImsStack/java/Android.bp),$(wildcard packages/modules/ImsMedia/sepolicy/system_ext/private/imsmedia.te),$(wildcard packages/apps/CarrierSettings/carrier_settings.mk)),)

# ImsMedia (RTP/RTCP + AMR/AMR-WB), CarrierSettings (IMS carrier data)
$(call inherit-product, packages/modules/ImsMedia/imsmedia.mk)
$(call inherit-product, packages/apps/CarrierSettings/carrier_settings.mk)

# ImsStack (SIP/IMS, ImsService), IWLAN (ePDG) and QNS
PRODUCT_PACKAGES += \
    ImsStack \
    Iwlan \
    QualifiedNetworksService

# Android 16 QPR2 telephony API compatibility for the Android 17 ImsStack
$(call soong_config_set_bool,imsstack_namespace,use_android16_telephony_compat,true)

# Overlays
PRODUCT_PACKAGES += \
    CarrierConfigOverlayImsNL \
    FrameworkResOverlayImsStack \
    ImsStackOverlayExynos2100 \
    TelephonyOverlayImsStack

endif
