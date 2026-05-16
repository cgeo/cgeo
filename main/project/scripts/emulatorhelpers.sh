#!/usr/bin/env bash
# Android Emulator helper functions for local development.
# Source this file in your shell (e.g., `. ./emulatorhelpers.sh`).
#
# Prerequisites:
#   - adb         on PATH (from Android SDK platform-tools/)
#   - Android SDK with the 'emulator' component installed;
#                 location resolved via $ANDROID_HOME, $ANDROID_SDK_ROOT,
#                 or one of the standard default paths
#                 (only required for emu_start / emu_restart)
#
# Quick reference:
#   emu_list
#   emu_start   [AVD_NAME|--select] [--quick-boot]
#   emu_stop    [AVD_NAME|SERIAL|--select]
#   emu_restart [AVD_NAME|--select] [--quick-boot]
#   emu_fix_dns [AVD_NAME|SERIAL|--select] [PACKAGE]
#   emu_fix_net [AVD_NAME|SERIAL|--select] [PACKAGE]
#   emu_help

# ------------------------------------------------------------------
# Private helpers
# ------------------------------------------------------------------

_emu_fail() { echo "ERROR: $*" >&2; return 1; }
_emu_warn() { echo "WARNING: $*" >&2; }
_emu_info() { echo "$*"; }

# DNS servers injected when starting an emulator to prevent connectivity issues.
_EMU_DNS_SERVERS="8.8.8.8,8.8.4.4"

# Find the Android SDK root directory.
_emu_sdk_root() {
  local sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
  if [[ -z "$sdk" ]]; then
    local candidate
    for candidate in \
        "$HOME/Library/Android/sdk" \
        "$HOME/Android/Sdk" \
        "/opt/android-sdk"; do
      if [[ -d "$candidate" ]]; then
        sdk="$candidate"
        break
      fi
    done
  fi
  if [[ -z "$sdk" ]] || [[ ! -d "$sdk" ]]; then
    _emu_fail "Android SDK not found. Set ANDROID_HOME or ANDROID_SDK_ROOT."
    return 1
  fi
  printf "%s" "$sdk"
}

# Resolve the path to the emulator binary.
_emu_binary() {
  local sdk
  sdk="$(_emu_sdk_root)" || return 1
  local bin="${sdk}/emulator/emulator"
  if [[ ! -x "$bin" ]]; then
    _emu_fail "Emulator binary not found at: $bin"
    return 1
  fi
  printf "%s" "$bin"
}

# List all available AVD names, one per line.
_emu_list_avds() {
  local emu
  emu="$(_emu_binary)" || return 1
  "$emu" -list-avds 2>/dev/null | tr -d '\r'
}

# List running emulators as lines: "serial|avd_name"
_emu_running_entries() {
  local serial state avd_name
  while IFS=$'\t ' read -r serial state _rest; do
    if [[ ! "$serial" =~ ^emulator- ]] || [[ "$state" != "device" ]]; then
      continue
    fi
    avd_name="$(adb -s "$serial" emu avd name 2>/dev/null | head -1 | tr -d '\r')"
    printf "%s|%s\n" "$serial" "${avd_name:-?}"
  done < <(adb devices 2>/dev/null | tr -d '\r' | tail -n +2)
}

# Get the running adb serial for a given AVD name. Returns 1 if not running.
_emu_serial_of_avd() {
  local target_avd="$1"
  local serial avd_name
  while IFS='|' read -r serial avd_name; do
    if [[ "$avd_name" == "$target_avd" ]]; then
      printf "%s" "$serial"
      return 0
    fi
  done < <(_emu_running_entries)
  return 1
}

# Get the AVD name for a running adb serial. Returns 1 if not found.
_emu_avd_of_serial() {
  local target_serial="$1"
  local serial avd_name
  while IFS='|' read -r serial avd_name; do
    if [[ "$serial" == "$target_serial" ]]; then
      printf "%s" "$avd_name"
      return 0
    fi
  done < <(_emu_running_entries)
  return 1
}

# Prompt the user to choose from all available AVDs (running ones are marked).
# Prints the chosen AVD name to stdout. Returns 1 if aborted.
_emu_prompt_avd() {
  local prompt_msg="${1:-Select an AVD:}"
  local -a avds=()
  local -a running_avds=()
  local serial avd_name avd

  while IFS='|' read -r serial avd_name; do
    running_avds+=("$avd_name")
  done < <(_emu_running_entries)

  while IFS= read -r avd; do
    [[ -n "$avd" ]] && avds+=("$avd")
  done < <(_emu_list_avds)

  if [[ ${#avds[@]} -eq 0 ]]; then
    _emu_fail "No AVDs found. Create one in Android Studio first."
    return 1
  fi

  echo "$prompt_msg" >&2
  local i r running_indicator
  for ((i=0; i<${#avds[@]}; i++)); do
    running_indicator=""
    for r in "${running_avds[@]}"; do
      [[ "$r" == "${avds[$i]}" ]] && running_indicator=" [running]" && break
    done
    printf "  %d) %s%s\n" "$((i+1))" "${avds[$i]}" "$running_indicator" >&2
  done
  printf "  0) Abort\n" >&2
  printf "Choice [0-%d]: " "${#avds[@]}" >&2

  local choice
  read -r choice
  if [[ "$choice" == "0" ]] || [[ -z "$choice" ]]; then
    echo "Aborted." >&2
    return 1
  fi
  if ! [[ "$choice" =~ ^[0-9]+$ ]] || [[ "$choice" -gt "${#avds[@]}" ]]; then
    _emu_fail "Invalid choice."
    return 1
  fi
  printf "%s" "${avds[$((choice-1))]}"
}

# Prompt the user to choose from currently running emulators.
# Prints the chosen adb serial to stdout. Returns 1 if aborted or none running.
_emu_prompt_running() {
  local prompt_msg="${1:-Select a running emulator:}"
  local -a serials=()
  local -a names=()
  local serial avd_name

  while IFS='|' read -r serial avd_name; do
    serials+=("$serial")
    names+=("$avd_name")
  done < <(_emu_running_entries)

  if [[ ${#serials[@]} -eq 0 ]]; then
    _emu_fail "No running emulators found."
    return 1
  fi

  echo "$prompt_msg" >&2
  local i
  for ((i=0; i<${#serials[@]}; i++)); do
    printf "  %d) %s  (%s)\n" "$((i+1))" "${names[$i]}" "${serials[$i]}" >&2
  done
  printf "  0) Abort\n" >&2
  printf "Choice [0-%d]: " "${#serials[@]}" >&2

  local choice
  read -r choice
  if [[ "$choice" == "0" ]] || [[ -z "$choice" ]]; then
    echo "Aborted." >&2
    return 1
  fi
  if ! [[ "$choice" =~ ^[0-9]+$ ]] || [[ "$choice" -gt "${#serials[@]}" ]]; then
    _emu_fail "Invalid choice."
    return 1
  fi
  printf "%s" "${serials[$((choice-1))]}"
}

# Resolve which AVD name to start.
#   ""         → if one AVD exists use it, otherwise prompt
#   "--select" → always prompt
#   "NAME"     → validate and use that AVD name
# Prints AVD name to stdout.
_emu_resolve_avd_to_start() {
  local arg="${1:-}"

  if [[ "$arg" == "--select" ]]; then
    _emu_prompt_avd "Select an AVD to start:" || return 1
    return 0
  fi

  if [[ -n "$arg" ]]; then
    local found=0 avd
    while IFS= read -r avd; do
      [[ "$avd" == "$arg" ]] && found=1 && break
    done < <(_emu_list_avds)
    if [[ $found -eq 0 ]]; then
      echo "ERROR: AVD '$arg' not found. Available AVDs:" >&2
      _emu_list_avds | sed 's/^/  /' >&2
      return 1
    fi
    printf "%s" "$arg"
    return 0
  fi

  # Auto: one AVD → use it; multiple → prompt
  local -a avds=()
  local avd
  while IFS= read -r avd; do
    [[ -n "$avd" ]] && avds+=("$avd")
  done < <(_emu_list_avds)

  if [[ ${#avds[@]} -eq 0 ]]; then
    _emu_fail "No AVDs found."
    return 1
  elif [[ ${#avds[@]} -eq 1 ]]; then
    printf "%s" "${avds[0]}"
  else
    _emu_prompt_avd "Multiple AVDs available. Select one to start:" || return 1
  fi
}

# Resolve which running emulator (by adb serial) to target.
#   ""           → if one emulator running use it, otherwise prompt
#   "--select"   → always prompt from running emulators
#   "emulator-N" → validate serial
#   "AVD_NAME"   → look up serial by AVD name
# Prints adb serial to stdout.
_emu_resolve_running_serial() {
  local arg="${1:-}"

  if [[ "$arg" == "--select" ]]; then
    _emu_prompt_running "Select a running emulator:" || return 1
    return 0
  fi

  if [[ -n "$arg" ]]; then
    if [[ "$arg" =~ ^emulator-[0-9]+$ ]]; then
      # Treat as adb serial
      if _emu_avd_of_serial "$arg" >/dev/null 2>&1; then
        printf "%s" "$arg"
        return 0
      else
        _emu_fail "Emulator serial '$arg' is not currently running."
        return 1
      fi
    else
      # Treat as AVD name
      local serial
      serial="$(_emu_serial_of_avd "$arg")" || {
        _emu_fail "AVD '$arg' is not currently running."
        return 1
      }
      printf "%s" "$serial"
      return 0
    fi
  fi

  # Auto: one running → use it; multiple → prompt
  local -a serials=()
  local -a names=()
  local serial avd_name
  while IFS='|' read -r serial avd_name; do
    serials+=("$serial")
    names+=("$avd_name")
  done < <(_emu_running_entries)

  if [[ ${#serials[@]} -eq 0 ]]; then
    _emu_fail "No running emulators found."
    return 1
  elif [[ ${#serials[@]} -eq 1 ]]; then
    printf "%s" "${serials[0]}"
  else
    _emu_prompt_running "Multiple emulators running. Select one:" || return 1
  fi
}

# Wait until a booted emulator (by serial) reports sys.boot_completed=1.
_emu_wait_for_boot() {
  local serial="$1"
  local timeout="${2:-120}"
  local elapsed=0 boot_completed

  _emu_info "Waiting for $serial to finish booting (timeout: ${timeout}s)..."
  while [[ $elapsed -lt $timeout ]]; do
    boot_completed="$(adb -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')"
    if [[ "$boot_completed" == "1" ]]; then
      _emu_info "Emulator $serial is ready."
      return 0
    fi
    sleep 3
    elapsed=$((elapsed+3))
  done
  _emu_warn "Timed out waiting for $serial to finish booting."
  return 1
}

# ------------------------------------------------------------------
# Public functions
# ------------------------------------------------------------------

# emu_list
#
# List all available AVDs and indicate which ones are currently running
# (including their adb serial).
emu_list() {
  local -a running_serials=()
  local -a running_names=()
  local serial avd_name

  while IFS='|' read -r serial avd_name; do
    running_serials+=("$serial")
    running_names+=("$avd_name")
  done < <(_emu_running_entries)

  local -a avds=()
  local avd
  while IFS= read -r avd; do
    [[ -n "$avd" ]] && avds+=("$avd")
  done < <(_emu_list_avds)

  if [[ ${#avds[@]} -eq 0 ]]; then
    _emu_info "No AVDs found. Create one in Android Studio first."
    return 0
  fi

  local i found_serial status
  for avd in "${avds[@]}"; do
    found_serial=""
    for ((i=0; i<${#running_names[@]}; i++)); do
      if [[ "${running_names[$i]}" == "$avd" ]]; then
        found_serial="${running_serials[$i]}"
        break
      fi
    done
    if [[ -n "$found_serial" ]]; then
      status="[running: $found_serial]"
    else
      status="[stopped]"
    fi
    printf "  %-40s %s\n" "$avd" "$status"
  done
}

# emu_stop [AVD_NAME|SERIAL|--select]
#
# Stop a running emulator gracefully via adb.
# Waits until it has disappeared from the adb device list.
emu_stop() {
  local arg="${1:-}"
  local serial
  serial="$(_emu_resolve_running_serial "$arg")" || return 1

  local avd_name
  avd_name="$(_emu_avd_of_serial "$serial" 2>/dev/null)" || avd_name="$serial"

  _emu_info "Stopping emulator $serial (AVD: $avd_name) ..."
  adb -s "$serial" emu kill 2>/dev/null || true

  local attempts=0
  while [[ $attempts -lt 30 ]]; do
    local still_running
    still_running="$(adb devices 2>/dev/null | grep -c "^${serial}[[:space:]]" || true)"
    [[ "$still_running" -eq 0 ]] && break
    sleep 2
    attempts=$((attempts+1))
  done

  if adb devices 2>/dev/null | grep -q "^${serial}[[:space:]]"; then
    _emu_warn "Emulator $serial may still be running after timeout."
  else
    _emu_info "Emulator $serial (AVD: $avd_name) has stopped."
  fi
}

# emu_start [AVD_NAME|--select] [--quick-boot]
#
# Start an emulator with the DNS servers pre-configured to avoid
# network connectivity issues (-dns-server $_EMU_DNS_SERVERS).
# Defaults to cold boot (-no-snapshot-load); pass --quick-boot to
# load from snapshot instead.
# Emulator output is redirected to a log file (path printed on start).
# Issues a warning (and does nothing) if the AVD is already running.
emu_start() {
  local arg="" quick_boot=0
  local param
  for param in "$@"; do
    case "$param" in
      --quick-boot) quick_boot=1 ;;
      *) arg="$param" ;;
    esac
  done

  local avd_name
  avd_name="$(_emu_resolve_avd_to_start "$arg")" || return 1

  # Warn and bail if already running
  local existing_serial
  existing_serial="$(_emu_serial_of_avd "$avd_name" 2>/dev/null)" || true
  if [[ -n "$existing_serial" ]]; then
    _emu_warn "AVD '$avd_name' is already running on $existing_serial. Nothing to do."
    return 0
  fi

  local emu_bin
  emu_bin="$(_emu_binary)" || return 1

  local boot_mode_flag boot_mode_desc
  if [[ $quick_boot -eq 1 ]]; then
    boot_mode_flag=""
    boot_mode_desc="quick boot (snapshot)"
  else
    boot_mode_flag="-no-snapshot-load"
    boot_mode_desc="cold boot"
  fi

  # Redirect emulator output to a log file so the terminal is not flooded.
  # (Android Studio similarly detaches the emulator process from the console.)
  local safe_name="${avd_name// /_}"
  local log_file="/tmp/emulator-${safe_name}.log"

  _emu_info "Starting AVD '$avd_name' [$boot_mode_desc] with DNS servers: $_EMU_DNS_SERVERS"
  _emu_info "Emulator output → $log_file"

  # nohup + disown fully detaches the process from this terminal session.
  # shellcheck disable=SC2086
  nohup "$emu_bin" -avd "$avd_name" -dns-server "$_EMU_DNS_SERVERS" $boot_mode_flag \
      >"$log_file" 2>&1 &
  disown $!

  # Wait for the emulator to register with adb
  local serial="" attempts=0
  while [[ $attempts -lt 20 ]]; do
    sleep 2
    serial="$(_emu_serial_of_avd "$avd_name" 2>/dev/null)" || true
    [[ -n "$serial" ]] && break
    attempts=$((attempts+1))
  done

  if [[ -z "$serial" ]]; then
    _emu_warn "Emulator process launched but not yet visible to adb. It may still be starting up."
    _emu_info "Check log: tail -f $log_file"
    return 0
  fi

  _emu_info "Emulator registered as $serial."
  _emu_wait_for_boot "$serial" || true
  _emu_info "AVD '$avd_name' is up on $serial."
}

# emu_restart [AVD_NAME|--select] [--quick-boot]
#
# Restart an emulator: shut it down, then start it again (with the DNS hack).
# Defaults to cold boot; pass --quick-boot to load from snapshot instead.
# If the specified (or auto-detected) AVD is not currently running, just start it.
emu_restart() {
  local arg="" quick_boot=0
  local param
  for param in "$@"; do
    case "$param" in
      --quick-boot) quick_boot=1 ;;
      *) arg="$param" ;;
    esac
  done

  local serial="" avd_name=""

  if [[ -n "$arg" ]] && [[ "$arg" != "--select" ]]; then
    # Specific AVD name or serial given: suppress expected "not running" errors.
    serial="$(_emu_resolve_running_serial "$arg" 2>/dev/null)" || true
    if [[ -z "$serial" ]]; then
      _emu_info "AVD '$arg' is not running — starting it now."
      if [[ $quick_boot -eq 1 ]]; then emu_start "$arg" --quick-boot
      else emu_start "$arg"; fi
      return $?
    fi
    avd_name="$(_emu_avd_of_serial "$serial" 2>/dev/null)" || avd_name="$arg"
  else
    # No specific target (or --select): check running emulators first.
    local -a run_serials=() run_names=()
    local s n
    while IFS='|' read -r s n; do
      run_serials+=("$s"); run_names+=("$n")
    done < <(_emu_running_entries)

    if [[ ${#run_serials[@]} -eq 0 ]]; then
      # Nothing running: go straight to start, no error message needed.
      _emu_info "No running emulator found — starting one."
      if [[ $quick_boot -eq 1 ]]; then emu_start "$arg" --quick-boot
      else emu_start "$arg"; fi
      return $?
    elif [[ ${#run_serials[@]} -eq 1 ]] && [[ "$arg" != "--select" ]]; then
      # Exactly one running and no explicit --select: use it directly.
      serial="${run_serials[0]}"; avd_name="${run_names[0]}"
    else
      # Multiple running, or --select requested: show interactive prompt.
      # _emu_prompt_running writes the menu to stderr (visible) and prints
      # the chosen serial to stdout (captured). No 2>/dev/null here.
      serial="$(_emu_prompt_running "Select a running emulator to restart:")" || return 1
      avd_name="$(_emu_avd_of_serial "$serial" 2>/dev/null)" || avd_name="$serial"
    fi
  fi

  _emu_info "Shutting down emulator $serial (AVD: $avd_name) ..."
  emu_stop "$serial" || true

  sleep 1
  _emu_info "Restarting AVD '$avd_name' ..."
  if [[ $quick_boot -eq 1 ]]; then emu_start "$avd_name" --quick-boot
  else emu_start "$avd_name"; fi
}

# emu_fix_dns [AVD_NAME|SERIAL|--select] [PACKAGE]
#
# Repair DNS/network connectivity issues on a running emulator:
#   - Disable private DNS mode
#   - Cycle WiFi off/on
#   - Force-stop the app so it reconnects cleanly
# PACKAGE defaults to cgeo.geocaching.
emu_fix_dns() {
  local arg="${1:-}"
  local pkg="${2:-cgeo.geocaching}"
  local serial
  serial="$(_emu_resolve_running_serial "$arg")" || return 1

  local avd_name
  avd_name="$(_emu_avd_of_serial "$serial" 2>/dev/null)" || avd_name="$serial"

  _emu_info "Applying DNS/network fix on $serial (AVD: $avd_name), package: $pkg"

  adb -s "$serial" shell settings put global private_dns_mode off
  adb -s "$serial" shell svc wifi disable
  sleep 2
  adb -s "$serial" shell svc wifi enable
  adb -s "$serial" shell am force-stop "$pkg"

  _emu_info "Done. DNS/WiFi reset applied and '$pkg' force-stopped."
}

# emu_fix_net [AVD_NAME|SERIAL|--select] [PACKAGE]
#
# Comprehensive network reset for a running emulator. Applies every known
# fix in escalating order:
#   1. Disable private DNS       — stops DoT/DoH from intercepting requests
#   2. Clear HTTP proxy          — removes any stale proxy injected by IDE/tools
#   3. Flush DNS resolver cache  — purges stale/negative cached answers
#   4. Override net.dns* props   — forces 8.8.8.8/8.8.4.4 at the system level
#   5. Enable auto-time          — triggers NTP sync to correct clock skew
#   6. Sync clock with host      — fixes TLS certificate errors from time drift
#   7. Toggle airplane mode      — full network stack teardown and rebuild
#   8. Re-enable WiFi            — airplane mode sometimes leaves WiFi disabled
#   9. Force-stop app            — app reconnects on a completely clean stack
# PACKAGE defaults to cgeo.geocaching.
emu_fix_net() {
  local arg="${1:-}"
  local pkg="${2:-cgeo.geocaching}"
  local serial
  serial="$(_emu_resolve_running_serial "$arg")" || return 1

  local avd_name
  avd_name="$(_emu_avd_of_serial "$serial" 2>/dev/null)" || avd_name="$serial"

  _emu_info "Applying full network reset on $serial (AVD: $avd_name), package: $pkg"

  # 1. Disable private DNS (DNS-over-TLS/HTTPS can block or delay name resolution)
  _emu_info "  [1/8] Disabling private DNS mode ..."
  adb -s "$serial" shell settings put global private_dns_mode off

  # 2. Clear any stuck HTTP proxy (stale proxy settings survive reboots)
  _emu_info "  [2/8] Clearing HTTP proxy settings ..."
  adb -s "$serial" shell settings put global http_proxy :0

  # 3. Flush DNS resolver cache (Android 6+; silently ignored on older releases).
  #    Both stdout and stderr are suppressed — the command prints "Unknown command"
  #    to stdout on images that don't support it, which looks like an error.
  _emu_info "  [3/8] Flushing DNS resolver cache ..."
  adb -s "$serial" shell cmd connectivity flush-dns-cache >/dev/null 2>&1 || true

  # 4. Override per-interface DNS via system properties (emulators have the
  #    necessary permissions; silently ignored on hardened real devices)
  _emu_info "  [4/8] Overriding DNS servers to 8.8.8.8 / 8.8.4.4 ..."
  adb -s "$serial" shell setprop net.dns1 8.8.8.8 2>/dev/null || true
  adb -s "$serial" shell setprop net.dns2 8.8.4.4 2>/dev/null || true

  # 5. Enable automatic time sync (NTP) — prevents TLS certificate errors
  #    that arise when the emulator clock drifts from real time
  _emu_info "  [5/8] Enabling auto-time (triggers NTP sync) ..."
  adb -s "$serial" shell settings put global auto_time 1

  # 6. Immediately sync the system clock to the host's current UTC time.
  #    Format: MMDDhhmm[[CC]YY][.ss]  — understood by Android's toybox date.
  #    The command is best-effort: it prints the new time to stdout and may still
  #    return a non-zero exit code on some images even when it succeeds.
  _emu_info "  [6/8] Syncing system clock with host ..."
  adb -s "$serial" shell date "$(date -u +"%m%d%H%M%Y.%S")" >/dev/null 2>&1 || true

  # 7. Airplane-mode toggle: brings ALL network interfaces fully down then back
  #    up, clearing any stuck routing tables, socket state or DHCP leases.
  #    Android 12+ (API 31+): use "cmd connectivity airplane-mode" — the legacy
  #    am-broadcast for AIRPLANE_MODE is a protected broadcast on those versions
  #    and throws a SecurityException when sent from adb shell (uid 2000).
  _emu_info "  [7/8] Toggling airplane mode (full network stack reset) ..."
  if adb -s "$serial" shell cmd connectivity airplane-mode enable >/dev/null 2>&1; then
    sleep 3
    adb -s "$serial" shell cmd connectivity airplane-mode disable >/dev/null 2>&1 || true
  else
    # Fallback for Android < 12: settings change + broadcast
    adb -s "$serial" shell settings put global airplane_mode_on 1
    adb -s "$serial" shell am broadcast -a android.intent.action.AIRPLANE_MODE \
        --ez state true >/dev/null 2>&1 || true
    sleep 3
    adb -s "$serial" shell settings put global airplane_mode_on 0
    adb -s "$serial" shell am broadcast -a android.intent.action.AIRPLANE_MODE \
        --ez state false >/dev/null 2>&1 || true
  fi
  sleep 3

  # 8. Re-enable WiFi explicitly — some Android versions leave it disabled
  #    after airplane mode is turned off
  _emu_info "  [8/8] Re-enabling WiFi ..."
  adb -s "$serial" shell svc wifi enable
  sleep 2

  # 9. Force-stop the app so it opens new connections on the clean network stack
  _emu_info "  Force-stopping '$pkg' for a clean reconnect ..."
  adb -s "$serial" shell am force-stop "$pkg"

  _emu_info "Full network reset complete on $serial."
  _emu_info "Tip: if issues persist, 'emu_restart' (cold boot) is the most reliable fix."
}

# emu_help
# Print a summary of all available emulator helper functions.
emu_help() {
  cat <<'EOF'
Android Emulator Helper Functions
==================================
Source this file first:  . ./emulatorhelpers.sh

Prerequisites:
  - adb         must be on PATH (from Android SDK platform-tools/)
  - Android SDK with the 'emulator' component installed;
                location resolved via $ANDROID_HOME, $ANDROID_SDK_ROOT,
                or one of the standard default paths
                (~/.../Android/sdk, /opt/android-sdk, ...)
                (only required for emu_start / emu_restart)

  emu_list
      List all available AVDs and whether each is currently running
      (includes the adb serial for running ones).

  emu_start [AVD_NAME|--select] [--quick-boot]
      Start an emulator with DNS servers pre-configured to avoid connectivity
      issues (-dns-server 8.8.8.8,8.8.4.4).
      Defaults to cold boot (-no-snapshot-load). Pass --quick-boot to load
      from snapshot instead. Warns if already running.
      Emulator output is redirected to /tmp/emulator-<avd>.log (printed on start).

  emu_stop [AVD_NAME|SERIAL|--select]
      Stop a running emulator gracefully and wait for it to shut down.

  emu_restart [AVD_NAME|--select] [--quick-boot]
      Stop a running emulator and start it again (with the DNS hack).
      Defaults to cold boot; pass --quick-boot to load from snapshot.
      If the emulator is not running, it is simply started.

  emu_fix_dns [AVD_NAME|SERIAL|--select] [PACKAGE]
      Quick DNS/network fix on a running emulator:
      disables private DNS, cycles WiFi off/on, and force-stops the app.
      PACKAGE defaults to cgeo.geocaching.

  emu_fix_net [AVD_NAME|SERIAL|--select] [PACKAGE]
      Comprehensive network reset — try this when emu_fix_dns is not enough:
        1. Disable private DNS (stops DoT/DoH interference)
        2. Clear stuck HTTP proxy
        3. Flush DNS resolver cache
        4. Override net.dns* system properties to 8.8.8.8/8.8.4.4
        5. Enable auto-time to trigger NTP sync
        6. Sync system clock with host (fixes TLS errors from clock drift)
        7. Toggle airplane mode (full network stack teardown and rebuild)
        8. Re-enable WiFi
        9. Force-stop the app for a clean reconnect
      PACKAGE defaults to cgeo.geocaching.
      If issues persist after this, use emu_restart (cold boot).

Emulator selection (applies to emu_start, emu_stop, emu_restart, emu_fix_dns, emu_fix_net):
  (no argument)   Auto: if exactly one applicable emulator/AVD exists, use it.
                  If multiple exist, display a numbered list and prompt.
  AVD_NAME        Target the named AVD (e.g. Pixel_8_API_35).
  SERIAL          Target the running emulator with this adb serial (emulator-5554).
  --select        Always show the selection list and prompt.
                  Includes an "Abort" option.

Examples:
  emu_list
  emu_start
  emu_start Pixel_8_API_35
  emu_start Pixel_8_API_35 --quick-boot
  emu_stop --select
  emu_restart --select
  emu_restart --quick-boot
  emu_fix_dns
  emu_fix_dns Pixel_8_API_35
  emu_fix_dns emulator-5554 com.example.myapp
  emu_fix_net
  emu_fix_net Pixel_8_API_35
  emu_fix_net emulator-5554 com.example.myapp
EOF
}


















