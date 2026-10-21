# Scaling Available Frequencies (Exynos2100)

See https://www.kernel.org/doc/Documentation/power/pm_qos_interface.txt

> Alternatively the user mode program could write a hex
> string for the value using 10 char long format e.g. "0x12345678".

## CPU Little Cluster (CPU0)

    2210000 -> 0x0021b8d0
    2106000 -> 0x00202290
    2002000 -> 0x001e8c50
    1898000 -> 0x001cf610
    1794000 -> 0x001b5fd0
    1690000 -> 0x0019c990
    1586000 -> 0x00183350
    1482000 -> 0x00169d10
    1378000 -> 0x001506d0
    1274000 -> 0x00137090
    1170000 -> 0x0011da50
    1066000 -> 0x00104410
      962000 -> 0x00eadd0
      858000 -> 0x00d1790
      754000 -> 0x00b8150
      650000 -> 0x009eb10
      533000 -> 0x0082208
      400000 -> 0x0061a80

## CPU Big Cluster (CPU4)

    2808000 -> 0x002ad8c0
    2704000 -> 0x00294280
    2600000 -> 0x0027ac40
    2496000 -> 0x00261600
    2392000 -> 0x00247fc0
    2288000 -> 0x0022e980
    2184000 -> 0x00215340
    2080000 -> 0x001fbd00
    1976000 -> 0x001e26c0
    1872000 -> 0x001c9080
    1768000 -> 0x001afa40
    1664000 -> 0x00196400
    1560000 -> 0x0017cdc0
    1456000 -> 0x00163780
    1352000 -> 0x0014a140
    1248000 -> 0x00130b00
    1144000 -> 0x001174c0
    1040000 -> 0x000fde80
      936000 -> 0x00e4840
      832000 -> 0x00cb200
      728000 -> 0x00b1bc0
      624000 -> 0x0098580
      533000 -> 0x0082208

## CPU Big Plus Cluster (CPU6)

    2808000 -> 0x002ad8c0
    2704000 -> 0x00294280
    2600000 -> 0x0027ac40
    2496000 -> 0x00261600
    2392000 -> 0x00247fc0
    2288000 -> 0x0022e980
    2184000 -> 0x00215340
    2080000 -> 0x001fbd00
    1976000 -> 0x001e26c0
    1872000 -> 0x001c9080
    1768000 -> 0x001afa40
    1664000 -> 0x00196400
    1560000 -> 0x0017cdc0
    1456000 -> 0x00163780
    1352000 -> 0x0014a140
    1248000 -> 0x00130b00
    1144000 -> 0x001174c0
    1040000 -> 0x000fde80
      936000 -> 0x00e4840
      832000 -> 0x00cb200
      728000 -> 0x00b1bc0
      624000 -> 0x0098580
      533000 -> 0x0082208

## GPU Scaling

    858000 -> 0x0000d1790
    767000 -> 0x0000bb418
    676000 -> 0x0000a50a0
    585000 -> 0x00008ed28
    494000 -> 0x0000789b0
    403000 -> 0x000062638
    312000 -> 0x00004c2c0
    221000 -> 0x000035f48
    130000 -> 0x00001fbd0

# Performance profiles

Settings > Battery > Performance profile (app `PerfProfiles`, also a Quick
Settings tile) stores the choice in `persist.sys.perf_profile`; init then
runs `/vendor/bin/perf-profile`, which applies it to this power HAL through the
Pixel power extension (`IPowerExt::setMode`). Numbering follows the old
LineageOS PerformanceManager.

| Value | Profile | What it does |
|-------|---------|--------------|
| 0 | Power save | `PROFILE_POWER_SAVE`: ufc max limit 1768000, GPU max 494000 |
| 1 | Balanced (default) | nothing; identical to the config without profiles |
| 2 | Performance | `vendor.powerhal.profile.performance=true`: LAUNCH adds a 1976000 ufc min limit for 2 s, INTERACTION one for 1.4 s |
| 3 | Efficiency | `PROFILE_EFFICIENCY`: ufc max limit 2184000, GPU max 585000 |

The ufc limits are "virtual" frequencies looked up in the exynos-ufcc tables of
the DT (columns: X1, A78, A55):

    max 2184000 -> 2184000 / 2184000 / 1794000
    max 1768000 -> 1768000 / 1768000 / 1690000
    min 1976000 -> 1976000 / 1976000 / 1170000

Boosts can't undo the caps: libperfmgr writes the value of the highest-priority
(lowest-index) active request, and the cap values sit above the boost values in
CPUMaxFreq (`1768000` > `2184000` > `2704000` CAMERA_LAUNCH > `2912000` LAUNCH);
min limits never exceed max limits in the kernel (the max wins). LOW_POWER
(Battery Saver) keeps its 1768000 cap in every profile and masks the extra
Performance boosts. GPUMaxFreq is only written while a capping profile is
active (`ResetOnInit: false`), so in Balanced gpu_max_clock is left alone.

Nothing may write `ufc/cpufreq_max_limit_strict`: while the ufc min limit is in a
"strict" row, exynos-ufcc uses max(cpufreq_max_limit, cpufreq_max_limit_strict)
as the max limit, which would lift the profile caps. Today only init chowns it.

The power HAL (uid root, group system, no dac_override) must be able to write
`/sys/kernel/gpu/gpu_max_clock` after boot: init.samsung.rc makes it
system:system 0664 (like gpu_min_clock) at boot_completed.
