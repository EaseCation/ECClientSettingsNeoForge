# Third-Party Notices

This document identifies third-party software used by the OBS privacy overlay in
EaseCation Client Settings. The notices below apply only to the identified
third-party material; they do not replace the license for the rest of this
project.

## OBS Overlay

- Project: OBS Overlay
- Author: zziger / Artem Dzhemesiuk
- Upstream: https://github.com/zziger/obs-overlay
- Imported baseline: `2.0.0-beta.3` for Minecraft 1.21.4
- Upstream commit: `f0079170add6b5b4dcc3807c741ebad752314a19`
- License: MIT
- Copyright: Copyright (c) 2024 Artem Dzhemesiuk

The OBS capture-order technique and portions of the native-hook integration are
adapted from OBS Overlay. The Minecraft 1.21.8 renderer integration has been
substantially changed for NeoForge's deferred GUI renderer and for coexistence
with Sodium, Iris, and ImmediatelyFast.

The complete upstream MIT license is distributed at
[`src/main/resources/META-INF/licenses/ecclientsettings/obs-overlay-LICENSE.txt`](src/main/resources/META-INF/licenses/ecclientsettings/obs-overlay-LICENSE.txt).

## MinHook 1.3.3

- Project: MinHook - The Minimalistic API Hooking Library for x64/x86
- Author: Tsuda Kageyu and contributors
- Upstream: https://github.com/TsudaKageyu/minhook
- Release: `v1.3.3`
- Release source commit: `9fbd087432700d73fc571118d6a9697a36443d88`
- License: BSD-2-Clause
- Copyright: Copyright (C) 2009-2017 Tsuda Kageyu

The Windows x64 and x86 binaries used by the OBS privacy overlay are byte-for-byte
identical to the DLLs in the official `MinHook_133_bin.zip` release archive:

| Binary | SHA-256 |
|---|---|
| `MinHook.x64.dll` | `BDDD6ADAEE8AB13EABAA7C73C97718CEE1437DB2054CA713EC7CC86E8002A300` |
| `MinHook.x86.dll` | `D1DB9AFDC79DCD34F77D1EB825C4F95E37E7F72CA7BD0E717E69D275FD94093E` |

MinHook contains Hacker Disassembler Engine 32 and Hacker Disassembler Engine
64 code by Vyacheslav Patkov. Those portions are also distributed under the
BSD-2-Clause terms and retain their separate copyright notices:

- Hacker Disassembler Engine 32 C, Copyright (c) 2008-2009, Vyacheslav Patkov.
- Hacker Disassembler Engine 64 C, Copyright (c) 2008-2009, Vyacheslav Patkov.

The complete MinHook license, including both HDE notices and disclaimers, is
distributed at
[`src/main/resources/META-INF/licenses/ecclientsettings/minhook-LICENSE.txt`](src/main/resources/META-INF/licenses/ecclientsettings/minhook-LICENSE.txt).

The names of upstream authors and projects are used only for attribution and do
not imply endorsement of EaseCation Client Settings.
