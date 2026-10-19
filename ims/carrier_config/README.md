# Per-carrier VoLTE for the open-source IMS stack

This directory makes `ims/overlays/CarrierConfigOverlayIms/res/xml/vendor.xml`, a
static RRO for `com.android.carrierconfig`. That file decides for which SIMs
VoLTE is switched on.

## Why it is needed

ImsStack (`packages/modules/ImsStack`) gets its IMS tuning (SIP timers, codecs,
IPsec, UT) from the CarrierSettings APK (`packages/apps/CarrierSettings`,
`org.lineageos.carriersettings`). CarrierSettings ships about 380 AOSP profiles
and about 1,300 Google profiles, but on purpose leaves out the service switches:
its README says "Service enablement stays with Android CarrierConfig". Its assets
are read by ImsStack only. They never reach Android's `CarrierConfigManager`.

The switch is `carrier_volte_available_bool` in Android's own carrier config.
ImsStack (`ServiceCaps`, `FeatureConfig`) and Telephony (`ImsManager`) both read
it from `CarrierConfigManager`. AOSP CarrierConfig sets it for only 68 carriers,
and the device needs `config_device_volte_available` as well. Without it you get
`isVolteEnabled=false`, all MmTel capabilities false, and calls over CS/CSFB.

`ImsProvisioningController: getTechsFromCarrierConfig failed` in the radio log
does not matter. It only means `ims.mmtel_requires_provisioning_bundle` has no
entry for that capability, so no provisioning is required (the normal case).

## How the effective config is built

`DefaultCarrierConfigService.loadConfig()` (packages/apps/CarrierConfig):

1. `assets/carrier_config_carrierid_<id>_*.xml`: specific carrier id first, then
   carrier id, then the MCC/MNC carrier id. If none exists,
   `carrier_config_mccmnc_<mccmnc>.xml` with filters.
2. `res/xml/vendor.xml` (this overlay replaces the empty AOSP file). Every
   `<carrier_config>` whose filters match is applied on top, in file order. So
   vendor.xml wins over the assets, and a later block wins over an earlier one.
   Filters: `mcc`, `mnc`, `gid1` (exact), `spn`/`imsi` (regex), `cid` (carrier
   id or specific carrier id). Attributes in one block are ANDed. The overlay is
   built with `--keep-raw-values`, otherwise `mnc="08"` turns into `8`.

CarrierConfigManager then puts this bundle on top of the platform defaults.
ImsStack reads that framework config as one layer of its own (between its AOSP
profiles and the Google profile, see ImsStack's README). Google profiles never
contain the keys written here.

## What is generated

`carrier_ims.py generate` writes vendor.xml in two parts:

1. **manual.xml, copied verbatim.** These are the hand-verified carriers. Today
   that is KPN 204-08 and 204-69 and Lebara 204-17: VoLTE + VoWiFi with ePDG/IKE
   values, UT off, preconditions off. No block is generated for these MCC/MNCs, so
   their effective config does not change when the generated part changes.
2. **Generated blocks**, for every carrier with a real IMS profile in
   CarrierSettings:
   - Google profile: the MCC/MNC-only entry in `carrier_settings/carrier_list.xml`
     (the one an MNO SIM without GID1/SPN/IMSI selectors gets) names a profile
     with at least one setting. One block per MCC/MNC, so MVNOs on that network
     get it too (just like KPN's block).
   - AOSP profile: `carrier_config_mccmnc_*.xml` or
     `carrier_config_override_mccmnc_*.xml` with settings (whole MCC/MNC), or
     `carrier_config_carrierid_<id>_*.xml` with settings (block filtered on
     mcc + mnc + cid, so only that carrier gets it).

   Each block sets:
   - `carrier_volte_available_bool=true`
   - `imsvoice.voice_qos_precondition_supported_bool=false`. This is a device
     fact: the Samsung RIL never reports dedicated-bearer QoS (`RILD:
     FillQosFlowInfo: QOS is not supported`, `qosSessionsLength[0]` during every
     KPN call), so a precondition offer would wait for a QoS that never comes.
     KPN, the verified carrier, runs exactly like this. The device-wide switch
     `config_imsstack_dedicated_bearer_qos_supported=false` would say the same
     thing, but it also changes ImsStack's config for KPN, so it is not used yet.
   - `carrier_supports_ss_over_ut_bool=false`, only in an extra mcc+mnc+cid block
     for carriers whose AOSP CarrierConfig asset turns UT on while LineageOS'
     APN database (`vendor/apn`) has no XCAP APN for that MCC/MNC. Supplementary
     services then stay on CS, as they are today without IMS (the KPN reason).
   - **No VoWiFi.** No CarrierSettings or AOSP CarrierConfig profile carries ePDG
     data. To add VoWiFi for a carrier, test it and put a full block into
     manual.xml.

   It never overrides an explicit `carrier_volte_available_bool=false` from AOSP
   CarrierConfig (an extra cid block puts it back). `policy.json` leaves out test
   networks (MCC 000, 001, 999) and can exclude an MCC/MNC with a reason.

`volte_carriers.tsv` lists every generated block (MCC, MNC, carrier id, source
profile, UT handling, carrier names from the carrier id database). Use it to
answer "is my carrier covered?".

Not covered: carriers without an IMS profile in CarrierSettings (e.g. Turkcell
286-01, Türk Telekom 286-03, whose Google profiles are empty). Those are left
alone. Use manual.xml once someone tests one.

## Usage

Run from `device/samsung/universal2100-common` (Python 3.9+, standard library only):

```sh
# regenerate after editing manual.xml / policy.json or after a CarrierSettings,
# CarrierConfig, carrier id or APN database update
python3 ims/carrier_config/carrier_ims.py generate
python3 ims/carrier_config/carrier_ims.py generate --check   # CI / before commit

# the bundle com.android.carrierconfig returns for one SIM
python3 ims/carrier_config/carrier_ims.py effective \
    --carrierconfig ../../../packages/apps/CarrierConfig/assets \
    --vendor ims/overlays/CarrierConfigOverlayIms/res/xml/vendor.xml \
    --mcc 286 --mnc 02

# before/after proof for the SIMs in proof_sims.json ("expect" marks surprises)
python3 ims/carrier_config/carrier_ims.py diff --sims ims/carrier_config/proof_sims.json \
    --before-carrierconfig ../../../packages/apps/CarrierConfig/assets \
    --before-vendor old-vendor.xml \
    --after-vendor ims/overlays/CarrierConfigOverlayIms/res/xml/vendor.xml
```

Built artifacts work too (pass `--aapt2 out/host/linux-x86/bin/aapt2` before
the subcommand): `--before-vendor target_files.zip!VENDOR/overlay/CarrierConfigOverlayIms.apk`,
`--before-carrierconfig target_files.zip!SYSTEM_EXT/priv-app/CarrierConfig/CarrierConfig.apk`.
The emulator follows `DefaultCarrierConfigService` and `CarrierResolver` (carrier
id from `latest_carrier_id/carrier_list.textpb`). It does not model carrier apps
with carrier privileges or user/test overrides (`cmd phone cc`).

## Checking on a phone

```sh
adb shell getprop gsm.sim.operator.numeric
adb shell dumpsys carrier_config | grep -E 'carrier_volte_available_bool|voice_qos_precondition_supported_bool|ss_over_ut'
adb logcat -b radio -s ImsStack ImsManager ImsPhone
```

Then Settings > Network > SIM > "VoLTE" (Enhanced 4G LTE) must be on. Look for
`REGISTER ... 200 OK` from ImsStack and the VoLTE icon or `ims` in
`dumpsys telephony.registry`.

## Other devices

Everything except the precondition line is device-neutral. Check your RIL: if
it reports QoS sessions (`qosSessionsLength` > 0 during a VoLTE call), drop
`KEY_PRECONDITION` from the generated settings.
