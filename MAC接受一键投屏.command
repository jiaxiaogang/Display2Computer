#!/usr/bin/env bash
TTY_NAME="$(tty)"
cd "/Users/jia/Desktop/repos/Display2Computer" || exit 1
nohup "/Users/jia/Desktop/repos/Display2Computer/MacRun.sh" >/dev/null 2>&1 &
disown
/usr/bin/osascript <<EOF >/dev/null 2>&1 &
tell application "Terminal"
  repeat with w in windows
    repeat with t in tabs of w
      if tty of t is "$TTY_NAME" then
        close w
        return
      end if
    end repeat
  end repeat
end tell
EOF
exit 0
