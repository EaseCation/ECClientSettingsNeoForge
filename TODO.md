# ECClientSettings 未实现功能 TODO

本文只列出 `ECClientSettingsNeoForge` 尚未提供、但 AxolotlClient 已有对应行为的功能。
已经完成的功能不会重复列出。这里的“未实现”只表示当前模组内没有同类能力，不代表
每一项都适合 EaseCation，也不代表其他整合包模组没有相似功能。

下文把 AxolotlClient 简称为“参考端”。每个勾选框只追踪一个玩家可感知的功能；仅有
配置字段或遗留文件、却没有接入当前 1.21 客户端运行链的内容不计入 TODO。

## 对比基线

- ECClientSettingsNeoForge：`master@b41b7304e95143fb3cb1c24ee19c47f692c4d4eb`
- AxolotlClient 当前开发分支：`dev@85c7e41521c5b1c2ff7300e719e93a0c7b71be2e`
- EC 既有设计使用的参考提交：
  `axolotlclient-reference-b1d066585626e4a7adf9f4ddbeb31cbf1ec3245f`
- AxolotlClient 的 `versions/1.21` 面向 Minecraft 1.21.1；本文比较玩家能看到的功能，
  不把 Fabric、NeoForge 或小版本内部实现差异当成功能缺口。

优先级含义：`高` 表示适合继续补进 EC 的通用客户端能力；`中` 表示有明确用途但需要
单独设计；`低` 表示装饰、边缘工具或已有替代方式；`生态` 表示依赖 AxolotlClient 后台、
指定服务器或其他模组，不能当作普通本地设置直接照搬。

## 已有功能仍缺少的独立能力

- [ ] **配置方案导出（Profile Export）** `中`：把一个配置方案打包成文件，方便备份、分享或迁移到另一台电脑。
- [ ] **配置方案导入（Profile Import）** `中`：从外部文件导入一个或多个配置方案，并处理名称冲突和损坏文件。
- [ ] **内置配置方案预设（Profile Presets）** `低`：从资源包提供的预设中一键建立并启用配置方案。
- [ ] **方块轮廓宽度（Block Outline Width）** `中`：让玩家调整选中方块轮廓线的粗细，而不只修改颜色和透明度。
- [ ] **动态彩虹受伤颜色（Chroma Hit Color）** `低`：让实体受伤闪烁颜色随时间循环变化；EC 当前只支持固定 ARGB 颜色。
- [ ] **盔甲受伤颜色控制（Hit Color on Armor）** `中`：独立决定自定义受伤颜色是否同时覆盖实体穿着的盔甲。
- [ ] **低盾牌（Low Shield）** `高`：在第一人称举盾时把盾牌模型下移，减少画面遮挡。
- [ ] **关闭动态视场角（Disable Dynamic FOV）** `高`：关闭疾跑、飞行或速度变化造成的原版 FOV 缩放，同时保留基础视场角。
- [ ] **HUD 总开关（HUD Master Switch）** `高`：一次启用或隐藏所有 EC HUD，不必逐个关闭组件。
- [ ] **HUD 快捷切换键（Toggle HUD Key）** `高`：使用可改键的快捷键临时显示或隐藏全部 HUD。
- [ ] **HUD 编辑器快捷键（Open HUD Editor Key）** `中`：用独立快捷键直接打开 HUD 编辑器；EC 当前的右 Shift 先打开完整设置界面。
- [ ] **HUD 内容增长锚点（HUD Anchor Point）** `中`：指定 HUD 以左、右或中心为固定点，使动态文字和列表朝预期方向伸展。
- [ ] **按游戏按键统计 CPS（Keybinding CPS）** `中`：按当前“攻击”和“使用”按键统计点击，不只读取物理鼠标左右键。
- [ ] **单人模式隐藏延迟 HUD（Hide Ping in Singleplayer）** `低`：进入单人世界时自动隐藏延迟 HUD；EC 当前仍显示 `-- ms`。
- [ ] **装备 HUD 显示主手物品（Armor HUD Main-hand Item）** `中`：在盔甲旁显示主手物品及背包内同类物品总数，并可选择放在列表两端或隐藏。
- [ ] **装备 HUD 排列方向（Armor HUD Direction）** `中`：让装备按上、下、左、右四个方向排列，而不是固定为竖排。
- [ ] **装备顺序反转（Reverse Armor Order）** `低`：切换头盔到靴子或靴子到头盔的显示顺序。
- [ ] **保护附魔等级（Protection Level）** `低`：在装备图标上标出保护附魔等级。
- [ ] **原始耐久数值（Durability Numbers）** `中`：显示剩余耐久、最大耐久或“剩余/最大”数值；EC 当前只显示百分比。
- [ ] **耐久数值颜色（Durability Number Color）** `低`：为装备 HUD 的耐久文字设置固定颜色，或继续跟随物品耐久颜色。
- [ ] **药水 HUD 排列方向（Potions HUD Direction）** `中`：让药水效果列表按上、下、左、右方向排列。
- [ ] **药水 HUD 显示模式（Potions HUD Mode）** `中`：在“仅图标”“仅文字”和“图标加文字”之间切换。
- [ ] **药水名称开关（Potion Effect Name Toggle）** `低`：保留持续时间时单独隐藏效果名称和等级。
- [ ] **药水计时颜色（Potion Timer Color）** `低`：为药水剩余时间设置独立文字颜色。
- [ ] **任意按键显示（Custom Keystrokes）** `中`：在按键 HUD 中新增玩家选择的键位，而不是只能显示移动、跳跃和鼠标键。
- [ ] **特殊按键图形（Special Keystrokes）** `低`：用专用图形显示空格、鼠标移动等特殊输入。
- [ ] **单键自定义标签（Keystroke Labels）** `低`：分别修改每个按键的文字、对齐方式，并可让标签自动跟随改键。
- [ ] **单键尺寸调整（Keystroke Size）** `低`：分别调整每个按键方块的宽度和高度。
- [ ] **单键位置调整（Keystroke Position）** `中`：在按键 HUD 内单独拖动和吸附每个按键。
- [ ] **自定义图形按键（Custom Graphics Keystrokes）** `低`：给按键选择图形和图形大小，不局限于文字标签。
- [ ] **自定义文字 HUD（Custom Text HUD）** `中`：创建玩家自己填写内容的文字组件，而不局限于预置 HUD 类型。
- [ ] **HUD 位置联动（HUD Dependency Links）** `低`：把多个 HUD 的位置关系连接起来，移动一个组件时保持它们之间的布局关系。
- [ ] **HUD 圆角背景（Rounded HUD Background）** `低`：为各个 HUD 独立设置圆角背景和圆角大小。

## 移动与视角

- [ ] **自由视角（Freelook）** `高`：旋转镜头时保持玩家身体和移动方向不变，并支持按住或切换两种触发方式。
- [ ] **临时切换视角（Snap Perspective）** `中`：按住快捷键时临时切到指定第一或第三人称视角，松开后恢复原视角。
- [ ] **切换式疾跑（Toggle Sprint）** `高`：按一次快捷键持续疾跑，再按一次关闭；它与 EC 现有的强制疾跑行为不同。
- [ ] **切换式潜行（Toggle Sneak）** `高`：按一次快捷键持续潜行，再按一次关闭，并在不允许的服务器上自动禁用。

## 画面与世界显示

- [ ] **移除暗角（Remove Vignette）** `中`：关闭屏幕边缘随亮度变化出现的原版暗角。
- [ ] **轻微视角晃动（Minimal View Bobbing）** `中`：减少走路时镜头和手部晃动，而不是只能在原版“开/关”之间选择。
- [ ] **关闭受伤镜头晃动（No Hurt Cam）** `高`：玩家受伤时不再让镜头倾斜摇晃，但不改变伤害结果。
- [ ] **隐藏降雨（No Rain）** `中`：本地不绘制雨雪天气，服务器天气和游戏逻辑保持不变。
- [ ] **扁平掉落物（Flat Items）** `低`：让世界中的掉落物使用扁平图标式显示，而不是常规立体物品模型。
- [ ] **运动模糊（Motion Blur）** `中`：在世界画面或界面中应用可调强度的运动模糊后处理。
- [ ] **按类型隐藏粒子（Particle Visibility）** `中`：分别决定每一种原版粒子是否显示；EC 当前的粒子功能只负责 ViaBedrock 粒子性能优化。
- [ ] **按类型增加粒子数量（Particle Multiplier）** `中`：为每一种粒子设置 1 到 20 倍的生成数量。
- [ ] **按类型修改粒子颜色（Particle Color）** `中`：为指定粒子设置自定义颜色和透明度。
- [ ] **强制暴击粒子（Always Crit Particles）** `低`：普通攻击也可显示暴击或附魔命中粒子。
- [ ] **信标光柱开关（Beacon Beam Toggle）** `低`：独立决定本地是否显示信标光柱；EC 的 OBS 选项只控制直播画面隐私。
- [ ] **末地折跃门光柱开关（End Gateway Beam Toggle）** `低`：独立决定本地是否显示末地折跃门产生的光柱。
- [ ] **显示自己的名称标签（Own Nametag）** `中`：在合适视角下显示本地玩家自己的头顶名称。
- [ ] **名称标签阴影（Nametag Shadow）** `低`：控制玩家名称标签文字是否带阴影。
- [ ] **名称标签背景（Nametag Background）** `低`：控制玩家名称标签背后的半透明底色；EC 现有名称处理只服务于 OBS 隐私。

## 界面与客户端体验

- [ ] **隐藏聊天栏（Hide Chat）** `中`：用快捷键临时隐藏聊天记录显示，再次切换后恢复。
- [ ] **断开连接确认（Confirm Disconnect）** `中`：点击“断开连接”时先显示确认窗口，减少误操作离开服务器。
- [ ] **标题界面 HUD 入口（Title Screen HUD Entry）** `中`：在标题界面直接打开 HUD 编辑器。
- [ ] **暂停界面 HUD 入口（Pause Menu HUD Entry）** `中`：在游戏暂停界面直接打开 HUD 编辑器。
- [ ] **设置入口显示策略（Settings Button Mode）** `低`：分别控制标题界面和暂停界面的设置按钮是隐藏、交给 Mod Menu，还是始终显示。
- [ ] **设置界面主题（Config UI Theme）** `低`：在不同配置界面样式之间切换，例如原版风格或圆角风格。
- [ ] **本地客户端窗口标题（Local Window Title）** `低`：由客户端本身改变窗口标题标识；EC 当前只接受服务器在玩家授权后下发标题。
- [ ] **客户端品牌名称（Client Brand）** `低`：在客户端品牌字符串中显示自定义客户端名称。
- [ ] **加载界面颜色（Loading Screen Color）** `低`：自定义进入游戏时 Mojang 加载界面的背景颜色。
- [ ] **全局夜间界面（Night Mode）** `低`：降低部分界面背景亮度，让菜单在暗环境下更舒适。
- [ ] **备用图标控制（Alternate Icons）** `低`：允许关闭彩蛋或替代图标，始终使用标准客户端图标。
- [ ] **制作人员页面（Credits Screen）** `低`：提供独立的客户端制作人员和贡献者页面。
- [ ] **制作人员页面音乐（Credits BGM）** `低`：控制制作人员页面的背景音乐。
- [ ] **制作人员页面彩色背景（Credits Color Background）** `低`：控制制作人员页面的彩色背景效果。
- [ ] **详细调试日志（Verbose Debug Logging）** `低`：让玩家或开发者临时开启更详细的客户端诊断输出。
- [ ] **日期时间格式（Date/Time Format）** `低`：统一设置截图记录和真实时间 HUD 使用的日期时间格式。
- [ ] **外部模块加载（External Modules）** `低`：允许第三方模块通过 AxolotlClient 的模块接口加入设置和生命周期；EC 没有通用扩展模块系统。
- [ ] **服务器限制客户端功能（Server Feature Restrictions）** `中`：服务器可通过数据包临时禁用自由视角、时间修改、低火和全亮，断开后自动恢复本地设置。
- [ ] **跳过加载地形界面（Skip Terrain Loading Screen）** `低`：切换世界或维度时直接回到游戏画面，不停留在“加载地形”界面。
- [ ] **超长服务器名称（Long Server Names）** `低`：把服务器列表中的名称长度上限提高到 1024 个字符。
- [ ] **额外标题标语（Extra Splash Texts）** `低`：把客户端自己的短句加入标题界面的黄色闪烁标语池。
- [ ] **隐藏 Realms 通知（Hide Realms Notifications）** `低`：关闭标题界面的 Realms 通知区域。
- [ ] **隐藏已修改标记（Hide Modded Status）** `低`：让原版状态检测不再把客户端标记为“已修改”。

## 截图与实用工具

- [ ] **截图复制（Copy Screenshot）** `中`：截图后直接把图片复制到系统剪贴板。
- [ ] **截图删除（Delete Screenshot）** `中`：截图后直接删除刚生成的图片文件。
- [ ] **截图外部打开（Open Screenshot）** `低`：截图后调用系统默认图片程序打开文件。
- [ ] **截图后查看大图（View Screenshot）** `中`：截图后直接在游戏内打开图片查看界面。
- [ ] **截图动作自动执行（Automatic Screenshot Action）** `低`：每次截图后自动执行玩家选定的复制、删除、打开、查看或上传动作。
- [ ] **区域截图（Screenshot Crop）** `中`：截取当前画面后在游戏内拖动选择区域并保存裁剪结果。
- [ ] **本地截图图库（Screenshot Gallery）** `中`：在游戏内浏览本机截图、查看大图并执行常用操作。
- [ ] **截图反馈位置（Screenshot Result Mode）** `低`：选择在聊天栏、Toast 通知或两处同时显示截图结果。
- [ ] **截图 Toast 通知（Screenshot Toast）** `低`：用带截图预览和可配置边框颜色的 Toast 显示截图完成状态。
- [ ] **截图上传与分享（Screenshot Sharing）** `生态`：把截图上传到 AxolotlClient 服务并生成可分享内容，需要外部后台和账号体系。
- [ ] **好友共享图片图库（Friends Image Gallery）** `生态`：在图库中浏览好友允许访问的共享图片，需要外部图片服务和好友权限。
- [ ] **按链接查看远程图片（Remote Image Viewer）** `生态`：输入分享链接后在游戏内下载并查看远程图片。
- [ ] **保存远程图片（Save Remote Image）** `生态`：把正在查看的远程图片保存到本地截图目录。
- [ ] **纵向滚动物品提示（Vertical Tooltip Scrolling）** `高`：物品说明超出屏幕高度时，用滚轮上下移动内容。
- [ ] **横向滚动物品提示（Horizontal Tooltip Scrolling）** `中`：按住 Shift 滚轮时左右移动过宽的物品说明。
- [ ] **TNT 引信倒计时（TNT Fuse Timer）** `高`：在已点燃的 TNT 上显示剩余爆炸秒数，并随剩余时间改变颜色。
- [ ] **Discord 状态（Discord Rich Presence）** `中`：在 Discord 中显示游戏状态、游玩时长和可选服务器信息。

## 尚未实现的 HUD 组件

- [ ] **疾跑与潜行状态 HUD（Toggle Modifiers HUD）** `高`：显示当前疾跑、潜行、按住或切换状态；没有状态时可隐藏或显示自定义占位文字。
- [ ] **服务器地址 HUD（Server IP HUD）** `中`：显示当前服务器地址，并可附带服务器图标。
- [ ] **客户端图标 HUD（Icon HUD）** `低`：在游戏画面或菜单中显示可移动的客户端图标。
- [ ] **移动速度 HUD（Speed HUD）** `高`：实时显示玩家或载具每秒移动的方块数。
- [ ] **坐标 HUD（Coordinates HUD）** `高`：显示坐标、方向、可选生物群系和主世界/下界坐标换算。
- [ ] **箭矢数量 HUD（Arrow HUD）** `中`：显示当前可使用的箭矢类型和数量，并可在没有箭时自动隐藏。
- [ ] **物品增减记录 HUD（Item Update HUD）** `中`：短时间列出背包中新获得和失去的物品及数量。
- [ ] **真实时间 HUD（Real Time HUD）** `中`：按玩家选择的格式显示本机日期和时间。
- [ ] **攻击距离 HUD（Reach HUD）** `中`：在攻击后短暂显示玩家到目标碰撞箱的距离。
- [ ] **内存 HUD（Memory HUD）** `中`：显示 Java 内存占用、分配量和可选变化图表。
- [ ] **在线人数 HUD（Player Count HUD）** `中`：显示当前客户端已知的在线玩家数量。
- [ ] **方向罗盘 HUD（Compass HUD）** `中`：用横向刻度显示朝向、角度和主要方位。
- [ ] **TPS HUD（Ticks Per Second HUD）** `中`：显示服务器 Tick 速率；EC 需要先确定可信的服务器数据来源或明确标为估算值。
- [ ] **连击计数 HUD（Combo HUD）** `高`：连续命中同一目标时累计次数，被命中或超时后重置。
- [ ] **鼠标移动 HUD（Mouse Movement HUD）** `低`：用图形显示当前鼠标移动方向和幅度。
- [ ] **世界天数 HUD（Day Counter HUD）** `低`：根据世界时间显示已经过去的游戏天数。
- [ ] **背包内容 HUD（Inventory HUD）** `中`：不打开背包也能查看主要背包格子和物品数量。
- [ ] **经验值 HUD（XP HUD）** `中`：显示等级、升级百分比和累计经验值，可选择显示组合。
- [ ] **玩家列表 HUD（Tab Overlay HUD）** `中`：允许移动和定制 Tab 玩家列表，并支持数字延迟、头像、页眉和页脚开关。
- [ ] **字幕 HUD（Subtitles HUD）** `中`：允许移动和定制原版声音字幕区域。
- [ ] **动作栏 HUD（Action Bar HUD）** `中`：允许移动动作栏，并调整消息停留时间和颜色。
- [ ] **Boss 血条 HUD（Boss Bar HUD）** `中`：允许移动 Boss 血条，并分别控制文字和血条显示。
- [ ] **准星 HUD（Crosshair HUD）** `高`：提供自定义准星、目标颜色和攻击冷却指示；该项已在 EC 路线中明确延期。
- [ ] **调试计数 HUD（Debug Counters HUD）** `低`：独立显示区块、实体和粒子数量，不必展开完整 F3 页面。
- [ ] **快捷栏 HUD（Hotbar HUD）** `低`：允许重新定位原版快捷栏区域。
- [ ] **计分板 HUD（Scoreboard HUD）** `中`：允许移动和定制原版侧边计分板的背景、标题、分数和透明度。
- [ ] **资源包 HUD（Pack Display HUD）** `低`：显示当前启用的资源包名称和图标。
- [ ] **玩家模型 HUD（Player HUD）** `中`：在 HUD 中显示会跟随玩家动作和朝向变化的本地角色模型。

## 账号、徽章与社交生态

以下功能依赖 AxolotlClient 自有服务、Microsoft 登录流程或联机辅助模组。若 EC 需要同类
能力，应先设计独立的服务、隐私和账号安全合同。

- [ ] **Microsoft 账号切换（Microsoft Account Switcher）** `生态`：在标题界面管理多个 Microsoft 账号并切换当前游戏会话。
- [ ] **离线账号切换（Offline Account Switcher）** `生态`：添加和切换本地离线账号。
- [ ] **标题界面账号入口（Title Screen Account Entry）** `生态`：在标题界面显示当前账号头像，并直接打开账号切换器。
- [ ] **皮肤库（Skin Library）** `生态`：浏览 Microsoft 账号已有皮肤，预览并装备选中的皮肤。
- [ ] **皮肤导入（Skin Import）** `生态`：从本地文件导入皮肤并选择经典或纤细手臂模型。
- [ ] **按玩家名下载皮肤（Download Player Skin）** `生态`：输入玩家名后下载该玩家的公开皮肤并加入本地皮肤库。
- [ ] **本地皮肤删除（Delete Local Skin）** `生态`：在皮肤库内删除本地皮肤文件及其元数据。
- [ ] **披风管理（Cape Manager）** `生态`：浏览账号披风、装备指定披风或选择不使用披风。
- [ ] **好友列表（Friends List）** `生态`：分别查看在线好友和全部好友，并可移除好友。
- [ ] **好友请求（Friend Requests）** `生态`：发送、接受、拒绝或取消好友请求。
- [ ] **用户屏蔽（User Blocking）** `生态`：查看已屏蔽用户，并执行屏蔽或解除屏蔽。
- [ ] **私聊（Direct Messages）** `生态`：在游戏内与好友进行一对一聊天。
- [ ] **聊天频道（Chat Channels）** `生态`：创建和管理多人频道，并在频道内收发消息。
- [ ] **频道邀请（Channel Invites）** `生态`：查看、接受或忽略聊天频道邀请。
- [ ] **聊天侧栏快捷键（Chat Sidebar Key）** `生态`：按快捷键在游戏内直接打开聊天侧栏。
- [ ] **好友与聊天快捷入口（Social Shortcut Buttons）** `生态`：在标题界面和暂停界面显示好友、聊天入口。
- [ ] **在线状态同步（Presence Updates）** `生态`：把当前服务器、游戏活动和在线状态同步给好友。
- [ ] **好友状态通知（Presence Notifications）** `生态`：好友切换在线状态或活动时显示客户端通知。
- [ ] **好友服务器加入（Friends Server Join）** `生态`：从好友列表查看可加入的服务器并直接连接。
- [ ] **好友加入权限（Friends Join Permission）** `生态`：由玩家决定好友能否根据自己的在线状态加入当前服务器。
- [ ] **World Host 联动（World Host Integration）** `生态`：通过 World Host 分享本地世界状态并让好友加入。
- [ ] **e4mc 联动（e4mc Integration）** `生态`：识别 e4mc 临时地址并把可加入信息同步给好友。
- [ ] **玩家徽章显示（Player Badges）** `生态`：在玩家头顶名称和 Tab 列表中显示 AxolotlClient 在线徽章。
- [ ] **自定义徽章文字（Custom Badge Text）** `生态`：在玩家名称旁显示玩家设置的自定义徽章文字。
- [ ] **Tab 徽章位置（Tab Badge Position）** `生态`：选择徽章位于名字前、对齐列或延迟图标前。
- [ ] **用户名管理（Username Management）** `生态`：查看或管理 AxolotlClient 账号关联的用户名信息。
- [ ] **账号数据导出（Account Data Export）** `生态`：将外部服务保存的账号数据导出为本地文件。
- [ ] **账号删除（Account Deletion）** `生态`：在客户端内发起删除 AxolotlClient 服务账号的请求。
- [ ] **更新通知（Update Notifications）** `生态`：外部服务发现新版本时在标题界面提示并提供下载页入口。
- [ ] **客户端新闻（Client News）** `生态`：从外部服务获取公告，并在游戏内的可滚动页面中查看。
- [ ] **隐私条款确认（Privacy Notice）** `生态`：首次启用外部服务前展示隐私说明，只有同意后才建立连接。
- [ ] **好友请求通知开关（Friend Request Notifications）** `生态`：决定收到好友请求时是否显示客户端通知。
- [ ] **频道邀请通知开关（Channel Invite Notifications）** `生态`：决定收到频道邀请时是否显示客户端通知。
- [ ] **注册时间可见性（Registration Date Visibility）** `生态`：决定其他用户能否看到该账号的注册时间。
- [ ] **历史用户名保留（Username History Retention）** `生态`：决定外部服务是否保存该账号的历史用户名。
- [ ] **最后在线可见性（Last Online Visibility）** `生态`：决定好友能否看到该账号最后在线的时间。
- [ ] **活动可见性（Activity Visibility）** `生态`：决定好友能否看到当前服务器和游戏活动详情。
- [ ] **好友图片访问权限（Friends Image Access）** `生态`：决定好友能否浏览该账号上传的图片。
- [ ] **外部服务详细日志（API Detailed Logging）** `生态`：为账号和社交网络请求开启更详细的诊断日志。
- [ ] **PluralKit 身份代理（PluralKit Proxy）** `生态`：根据 PluralKit 成员身份自动代理聊天显示，需要第三方令牌和隐私设计。

## Hypixel 与特定服务器功能

这些项目针对 Hypixel、BedWarsPractice、PvP Land 或 Minemen Club。它们不属于
EC 通用客户端设置，只有决定支持对应服务器生态后才应进入实现计划。

- [ ] **自动 GG（AutoGG）** `生态`：识别比赛结束消息后自动发送玩家配置的 `GG` 文本。
- [ ] **自动 GF（AutoGF）** `生态`：识别比赛结束消息后自动发送玩家配置的 `GF` 文本。
- [ ] **自动 GLHF（AutoGLHF）** `生态`：识别比赛开始消息后自动发送玩家配置的 `GLHF` 文本。
- [ ] **自动 Tip（AutoTip）** `生态`：按间隔自动执行 Hypixel Tip。
- [ ] **隐藏自动 Tip 回执（Hide AutoTip Messages）** `生态`：过滤自动 Tip 产生的重复系统回执。
- [ ] **好友上线自动 Boop（AutoBoop）** `生态`：好友上线时自动执行 `/boop`，并支持白名单或黑名单过滤。
- [ ] **等级头标（LevelHead）** `生态`：在玩家名称旁显示 Network、BedWars 或 SkyWars 等级。
- [ ] **本地昵称隐藏（Nick Hider）** `生态`：在聊天和界面中把自己或其他玩家的名字替换成通用名称。
- [ ] **本地皮肤隐藏（Skin Hider）** `生态`：在本地把自己或其他玩家的皮肤替换为默认皮肤。
- [ ] **SkyBlock 视角锁定（SkyBlock Rotation Lock）** `生态`：用快捷键临时锁定玩家视角旋转。
- [ ] **隐藏大厅加入消息（Hide Lobby Join Messages）** `生态`：过滤 Hypixel 大厅中的重复加入提示。
- [ ] **玩家数据查询命令（Player Stats Command）** `生态`：在客户端命令中查询 BedWars、SkyWars 和 Duels 数据。
- [ ] **BedWars 硬核爱心显示（BedWars Hardcore Hearts）** `生态`：己方床被破坏后把生命值图标改为原版硬核模式样式。
- [ ] **BedWars 饥饿栏控制（BedWars Hunger Display）** `生态`：决定 BedWars 中是否显示原版饥饿栏。
- [ ] **BedWars 护甲值栏控制（BedWars Armor Bar）** `生态`：决定 BedWars 中是否显示生命值上方的原版护甲值图标。
- [ ] **BedWars 等级头标（BedWars Level Head）** `生态`：在 BedWars 场景中按指定模式显示玩家等级。
- [ ] **BedWars 重复消息过滤（BedWars Message Filter）** `生态`：隐藏比赛中重复或干扰阅读的聊天提示。
- [ ] **BedWars 消息重排（BedWars Message Rewrite）** `生态`：把部分比赛消息改成更紧凑、统一的格式。
- [ ] **BedWars 聊天比赛时间（BedWars Chat Time）** `生态`：在比赛聊天消息前显示当前对局时间。
- [ ] **BedWars 自定义 Tab（BedWars Custom Tab）** `生态`：替换 BedWars 玩家列表内容，并分别控制页眉、页脚和延迟图标。
- [ ] **BedWars 队伍升级 HUD（Team Upgrades HUD）** `生态`：实时显示锋利、保护、陷阱等队伍升级状态。
- [ ] **BedWars 资源 HUD（Resources HUD）** `生态`：在比赛中统计和显示铁、金、钻石、绿宝石等资源。
- [ ] **BedWars 玩家数据面板（Stats Overlay）** `生态`：开局前或比赛中显示玩家等级、FKDR、KDR、WLR 和连胜。
- [ ] **BedWars 会话统计 HUD（Session Statistics HUD）** `生态`：统计当前客户端会话中的胜负、击杀、床和连胜数据。
- [ ] **BedWars 会话统计重置（Reset Session Statistics）** `生态`：用快捷键清空本次客户端会话累计的 BedWars 统计。

## 证据索引

AxolotlClient 的主要证据：

- `README.md`：公开功能列表。
- `common/src/main/java/io/github/axolotlclient/AxolotlClientConfigCommon.java`：通用画面、名称标签、徽章和界面选项。
- `versions/1.21/src/main/java/io/github/axolotlclient/config/AxolotlClientConfig.java`：1.21 的低盾牌、扁平物品、加载界面和夜间模式。
- `common/src/main/java/io/github/axolotlclient/AxolotlClientCommon.java` 与
  `versions/1.21/src/main/java/io/github/axolotlclient/AxolotlClient.java`：内置模块注册。
- `common/src/main/java/io/github/axolotlclient/modules/hud/HudManagerCommon.java` 与
  `versions/1.21/src/main/java/io/github/axolotlclient/modules/hud/HudManager.java`：HUD 组件注册。
- `common/src/main/java/io/github/axolotlclient/modules/hud/gui/hud/item/ArmorHud.java`、
  `common/src/main/java/io/github/axolotlclient/modules/hud/gui/hud/PotionsHud.java` 与
  `versions/1.21/src/main/java/io/github/axolotlclient/modules/hud/gui/hud/KeystrokeHud.java`：EC 已有 HUD 类型仍缺少的独立能力。
- `versions/1.21/src/main/java/io/github/axolotlclient/modules/screenshotUtils/ScreenshotUtils.java`：截图动作、反馈方式、裁剪和图库入口。
- `common/src/main/java/io/github/axolotlclient/api/Options.java`、
  `versions/1.21/src/main/java/io/github/axolotlclient/api/APIOptions.java` 与
  `versions/1.21/src/main/java/io/github/axolotlclient/modules/auth/Auth.java`：账号、社交、隐私和快捷入口。
- `common/src/main/java/io/github/axolotlclient/modules/hypixel/HypixelMods.java`：Hypixel 子功能注册。
- `common/src/main/java/io/github/axolotlclient/config/profiles/Profiles.java`：配置方案导入、导出和预设。
- `common/src/main/java/io/github/axolotlclient/util/FeatureDisablerCommon.java` 与
  `versions/1.21/src/main/java/io/github/axolotlclient/util/FeatureDisabler.java`：服务器功能限制。
- `versions/1.21/src/main/java/io/github/axolotlclient/mixin/`：配置项和模块接入游戏行为的最终证据。

EC 当前能力的主要证据：

- `src/main/java/net/easecation/clientsettings/client/ClientSettingsScreen.java`：当前设置分类和玩家可配置入口。
- `src/main/java/net/easecation/clientsettings/client/ClientSettingsKeyMappings.java`：当前五个客户端快捷键。
- `src/main/java/net/easecation/clientsettings/profile/model/HudWidgetId.java`：当前八种 HUD 组件。
- `src/main/java/net/easecation/clientsettings/profile/model/HudSettings.java`、
  `HudWidgetStyle.java` 与 `KeystrokesSettings.java`：当前 HUD 通用样式和按键 HUD 数据范围。
- `src/main/java/net/easecation/clientsettings/feature/`：当前画面、HUD、性能和 OBS 功能实现。
- `docs/pvp-client/00-roadmap-and-boundaries.md` 与
  `docs/pvp-client/10-hud-foundation-and-priority.md`：已实现范围和明确延期项。

## 未计入的遗留内容

- **自定义天空（Custom Skies）**：README 和 1.21 配置类仍保留名称，但当前 1.21 没有读取该开关的渲染代码；`CHANGELOG.md` 也明确记录 1.21.1 已移除该模块。
- **MCC Island 状态识别（MCC Island Presence）**：源码保留状态提供器，但当前 1.21 的内置模块注册没有初始化 `MccIslandMods`，聊天回包监听不会生效。
