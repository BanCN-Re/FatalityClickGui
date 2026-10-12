# Fatality Style ClickGUI

基于 **fatality.win** 经典炭黑扁平化风格设计的 Minecraft 现代化 ClickGUI 独立组件库。

---

## 🎨 视觉风格与特性

- **经典炭黑纯平界面**：深度定制的深灰/炭黑表面（`#151515` ~ `#1C1C1C`）与标志性珊瑚红/三文鱼亮色点缀（`#FF615A`）。
- **三列自适应流式瀑布流 (Masonry 3-Column)**：根据屏幕尺寸动态计算列数与列宽，自动计算最佳高度平衡布局。
- **左侧模块快速索引**：分类下所有模块在左侧导航栏实时高亮显示，点击快速平滑滚动定位。
- **全平滑阻尼动画 (Exponential Motion)**：帧率无关的指数渐进动画，丝滑过渡、不超调、不掉帧。
- **底部工具条与全局搜索**：
  - 支持快捷键 `Ctrl + F` 或点击搜索图标唤起输入框，实时按名称与配置过滤模块。
  - 一键快速保存配置（Config Save Toast 提示）。
  - 配置管理器模态框（加载、保存、删除自定义配置，打开配置文件夹）。
  - 客户端信息面板（FPS、内存使用率、客户端版本、语言切换、界面模式切换）。
- **丰富的参数行控件**：
  - 复选框（Checkbox，带弹性伸缩白色打勾动画）
  - 数值滑动条（Number Slider，支持拖拽滑块、滚轮微调以及右键直接输入数值）
  - 范围滑动条（Range Slider，双把手设置数值区间）
  - 单选/多选下拉菜单（Combo Dropdown，支持 Single Mode 与 MultiBool）
  - 完整颜色选择器（Color Picker，HSB 饱和度色板、垂直色相条、水平透明度条、HEX 文本双向编辑与复制粘贴）
  - 槽位分配器（Slot Picker，快捷栏 9 槽与副手槽位角色分配）
  - 文本输入框（Text Row，行内就地编辑）
  - 子设置抽屉（Gear Row，齿轮图标点击弹出二级配置浮层）
- **3D 玩家模型实时预览卡片 (Render 预览)**：
  - 实时绘制本地玩家模型（装备、手持物、皮肤）。
  - 支持光标悬停视线跟随、鼠标拖拽 360 度旋转。
  - 支持 ESP 外发光着色器特效（`fa_model.fsh`）及白边描边与 Nametag 预览。

---

## 📁 目录文件清单

```text
D:\Work\Fatality/
├── fonts/                      # 字体资源
│   ├── fa-solid.ttf            # FontAwesome 实心图标
│   ├── lucide.ttf              # Lucide 图标字体
│   ├── manrope-bold.ttf        # 标题与 Logo 字体
│   └── misans.ttf              # MiSans UI 文本字体
├── shaders/                    # 预览着色器
│   └── fa_model.fsh            # 玩家 ESP 外发光离屏合成着色器
├── FaClickGui.java             # ClickGUI 主界面 Screen
├── FaTheme.java                # 配色表、尺寸常量、图标字形与辅助函数
├── FaContext.java              # 单帧交互与渲染上下文（鼠标位置、悬浮状态、dt）
├── FaDraw.java                 # 绘图抽象词汇层（rect, outline, quad, shadow, text, item, clip 等）
├── CanvasFaDraw.java           # 基于 2D Canvas 的标准实现
├── FaFont.java                 # 字体度量与 Provider 接口
├── FaMotion.java               # 指数阻尼动画平滑计算工具
├── FaTextInput.java            # 单行文本输入与光标选择工具
├── FaKeySink.java              # 键盘输入焦点接口
├── FaBox.java                  # 瀑布流容器抽象卡片
├── FaGroup.java                # 模块设置卡片（标题、Enabled 开关、参数行列表）
├── FaRow.java                  # 行控件基类
├── FaCheckRow.java             # 布尔勾选框行
├── FaEnableRow.java            # 模块主开关行（含按键绑定、隐藏眼睛图标）
├── FaSliderRow.java            # 数值滑动条行（支持拖动/微调/直接输入）
├── FaRangeRow.java             # 区间滑动条行
├── FaComboRow.java             # 单选/多选下拉列表行
├── FaColorRow.java             # 颜色色块行
├── FaTextRow.java              # 字符串文本行
├── FaKeyRow.java               # 单独按键绑定行
├── FaSlotRow.java              # 槽位分配行
├── FaGearRow.java              # 齿轮二级设置行
├── FaPopup.java                # 模态浮层基类
├── FaListPopup.java            # 下拉菜单弹出浮层
├── FaColorPopup.java           # 调色板弹出浮层
├── FaConfigPopup.java          # 配置管理弹出浮层
├── FaInfoPopup.java            # 客户端信息弹出浮层
├── FaRowsPopup.java            # 齿轮二级设置弹出浮层
├── FaSlotPopup.java            # 槽位分配弹出浮层
├── FaPreviewCard.java          # Render 标签页 3D 玩家模型预览卡片
└── FaPlayerModel.java          # 3D 玩家离屏渲染与着色器后处理
```

---

## 🛠️ 接入指南 (Integration Guide)

### 1. 调整 Package 路径
将所有 `.java` 文件顶部的 package 替换为你工程中的目标路径，例如：
```java
package your.client.ui.clickgui.fatality;
```

---

### 2. 模块与分类适配 (`Category` & `Module`)

`FaClickGui.java` 中通过以下方式加载模块：
```java
void loadModules(List<Module> visible) { ... }
```

如果你客户端的模块类和分类枚举名称不同，只需修改以下几处对应：
- `Category`: 需要具备 `name()`、`getDisplayName()`，以及对应 `COMBAT`, `MOVEMENT`, `RENDER`, `PLAYER`, `WORLD`, `MISC` 枚举值。
- `Module`: 需要提供：
  - `getName()`: 模块内部名称
  - `getDisplayName()`: 界面展示名称
  - `getCategory()`: 所属分类
  - `isEnabled()` / `toggle()`: 状态获取与切换
  - `getKey()` / `setKey(int key)`: 按键绑定获取与设置
  - `getValues()`: 返回该模块包含的所有 `Value<?>` 参数列表

---

### 3. 参数值系统适配 (`Value` 与子类)

`FaGroup.java` 的 `create(Value<?> value, Set<Value<?>> seen)` 负责把模块内的各种参数转换为对应的 UI 行。默认映射如下：

| 客户端 Value 类型 | 对应 UI 控件行 | 描述 |
| :--- | :--- | :--- |
| `BoolValue` | `FaCheckRow` | 普通布尔勾选框 |
| `KeyValue` | `FaKeyRow` | 独立按键绑定行 |
| `NumberValue` | `FaSliderRow` | 单数值滑条（支持 `getMin()`, `getMax()`, `getInc()`, `getValue()`） |
| `BoundsNumberValue` | `FaRangeRow` | 双把手区间滑条（`getLow()`, `getHigh()`） |
| `ModeValue` | `FaComboRow.single(...)` | 单选模式下拉框（`getAvailableModes()`, `getAvailableModeName()`） |
| `MultiBoolValue` | `FaComboRow.multi(...)` | 多选模式下拉框 |
| `ColorValue` | `FaColorRow` | 颜色拾取色块 |
| `TextValue` | `FaTextRow` | 字符串输入框 |
| `SlotValue` | `FaSlotRow` | 槽位分配器 |
| `GroupValue` | `FaGearRow` | 齿轮子设置抽屉（包含子 `Value` 列表） |

如果你的客户端 Value 体系使用了不同的类名，只需在 `FaGroup.java` 的 `create` 方法中将类型判断 `instanceof YourValueClass` 替换为你客户端的对应类即可。

---

### 4. 绘图系统适配 (`FaDraw` & `CanvasFaDraw`)

界面的所有绘制调用全部收敛在 `FaDraw.java` 这个抽象词汇层：
- `rect(x, y, w, h, radius, argb)`：带圆角的矩形填充
- `outline(x, y, w, h, radius, width, argb)`：描边
- `quad(x, y, w, h, tl, tr, br, bl)`：四角渐变色矩形
- `shadow(x, y, w, h, radius, sigma, argb)`：高斯柔和阴影
- `text(text, x, y, font, argb)`：文本渲染
- `clip(x, y, w, h)`：矩形裁剪范围
- `translate(x, y)`, `scale(s)`, `opacity(a)`：变换与透明度

#### 方案 A：使用客户端已有的 2D Canvas / NanoVG / Skia 引擎
直接保留 `CanvasFaDraw.java`，将类中的 `wtf.carnation.graphics.canvas.Canvas` 替换为您工程中的 Canvas 句柄即可。

#### 方案 B：使用 Minecraft 原生 `GuiGraphics`
如果不使用外部 2D 引擎，可以直接基于 `GuiGraphics` 实现 `FaDraw`：
```java
public class VanillaFaDraw extends FaDraw {
    private GuiGraphics graphics;

    public void begin(GuiGraphics graphics) {
        this.graphics = graphics;
    }

    @Override
    void rect(float x, float y, float w, float h, float radius, int argb) {
        graphics.fill((int) x, (int) y, (int) (x + w), (int) (y + h), argb);
    }

    @Override
    void text(String text, float x, float y, FaFont font, int argb) {
        graphics.drawString(Minecraft.getInstance().font, text, (int) x, (int) y, argb, false);
    }
    // ... 其余方法对应调用 graphics 的变换与 scissor
}
```

---

### 5. 字体与图标资源绑定 (`FaFont` & `FaTheme`)

在 `FaFont.java` 中：
- `FaFont.GAME` 默认绑定了四个字体：
  - `manropeBold`: 用于左上角 LOGO
  - `misans`: 用于常规标题、分类 Tab、标签、数值文字
  - `lucide`: 用于顶部与底部的所有功能图标
  - `fa-solid`: 备用 FontAwesome 图标库
- 将 `fonts/` 目录下的字体文件放置到你工程的 `resources/assets/<modid>/font/` 路径，并确保您的字体加载器（如 `FontLoader`）能正确读取即可。

---

### 6. 打开 GUI

在客户端按键监听（或 ClickGui 模块的 `onEnable`）中打开界面：
```java
Minecraft.getInstance().setScreen(new FaClickGui());
```

---

## ⚙️ 快捷键与交互

- `ESC`：逐层关闭已打开的模态弹窗；若无弹窗则关闭 ClickGUI。
- `Ctrl + F`：直接聚焦底部搜索框，按模块或设置名称模糊过滤。
- `鼠标滚轮`：滚动模块列表或侧边栏；在滑条上滚轮可进行步进微调。
- `右键滑动条`：直接进入数字编辑状态，输入数值后回车确认。
- `右键按键绑定框`：清除当前按键绑定（设置为 None）。
- `右下角拖拽柄`：按住右下角可自由调整窗口宽高尺寸；按住顶部标题栏可自由拖拽窗口位置。
- `位置记忆`：关闭界面时自动保存窗口位置比例与大小，下次打开自动还原。

