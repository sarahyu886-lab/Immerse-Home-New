package com.pico.welcomespace.ui.planner

import androidx.compose.ui.graphics.Color

/**
 * HF/IH Figma 全局 tokens（docs/figma-spec-pflow.md 第 1 节 + 节点 2302:39362 GLOBAL_VARS）。
 * 项目自绘颜色常量对象，颜色不写入 Material 主题。
 *
 * 玻璃面板收敛为两级 alpha（规范 2302:39362）：
 * - 弱玻璃 rgba(255,250,240,0.2) = fill_ef0f56d0（backdrop blur 14px）
 * - 强玻璃 rgba(255,255,255,0.75) = fill_80687602（backdrop blur 20px）
 * Compose 无 backdrop blur，用半透明浅色 + 白描边近似。
 */
object PlannerHfColors {
    /** 主文字 / 房间名 / 尺寸标注 #292722（≈规范中性深色 #292929 的暖棕映射） */
    val TextPrimary = Color(0xFF292722)

    /** 次文字（白面板上的说明文字）#59554D */
    val TextSecondary = Color(0xFF59554D)

    /** 输入框值区底色 / 画板底色 / PlanHandle 内芯 #FAF8F2 */
    val SurfaceCream = Color(0xFFFAF8F2)

    /** P01 标题 / 正文 深棕 #29211A */
    val DeepBrown = Color(0xFF29211A)

    /** toast / 弹窗标题正文 #292621 */
    val ToastText = Color(0xFF292621)

    /** 按钮文字 / 图标色 #382B1A（深琥珀上深棕字） */
    val ButtonText = Color(0xFF382B1A)

    /** 导航 Secondary 按钮文字 #FFFFFF */
    val NavTextLight = Color(0xFFFFFFFF)

    /** 画布选中对象高亮（墙/门/窗） #785510 */
    val SelectionHighlight = Color(0xFF785510)

    /** 墙体默认色 #6E6E6E */
    val Wall = Color(0xFF6E6E6E)

    /** 输入框描边（弱） #9D978C */
    val InputBorder = Color(0xFF9D978C)

    /** 辅助灰 / 弱化标注文字 #B8B8B8（规范中性灰级） */
    val AuxiliaryGray = Color(0xFFB8B8B8)

    /** 窗块底色 #D9D9D9 */
    val WindowPaneFill = Color(0xFFD9D9D9)

    /** 窗块描边 #292929 */
    val WindowPaneStroke = Color(0xFF292929)

    /** 门块底色 #F7F7F7 */
    val DoorLeafFill = Color(0xFFF7F7F7)

    /** 备用琥珀 #F8B856 */
    val AmberAlt = Color(0xFFF8B856)

    /** 弱玻璃面板（暖白 20%）rgba(255,250,240,0.2) = fill_ef0f56d0 */
    val GlassWeak = Color(0x33FFFAF0)

    /** Primary 按钮底 / 选中态 rgba(255,191,74,0.9) = #FFBF4A = fill_4009dbd8 */
    val PrimaryContainer = Color(0xE6FFBF4A)

    /** Secondary 按钮底（弱暖白玻璃 20%）= fill_ef0f56d0 */
    val SecondaryContainer = GlassWeak

    /** 按钮描边 #FFFFFF */
    val ButtonStroke = Color(0xFFFFFFFF)

    /** 画布面板底（弱暖白玻璃 20%，叠于主窗口强玻璃之上形成层叠）= fill_ef0f56d0 */
    val CanvasPanel = GlassWeak

    /** 强玻璃面板（白 75%）rgba(255,255,255,0.75) = fill_80687602 */
    val GlassStrong = Color(0xBFFFFFFF)

    /** 就近工具面板 / 弹窗底（强白玻璃 75%）= fill_80687602 */
    val ToolPanel = GlassStrong

    /** 弹窗描边 rgba(89,85,77,0.4) */
    val DialogStroke = Color(0x6659554D)

    /** 主工作窗口 Glass 底（强暖白玻璃 75%，规范强档 alpha + 品牌暖白相） */
    val GlassFill = Color(0xBFFFFAF0)

    /** 品牌标题衬底（强暖白玻璃 75%，与主窗口同档） */
    val HeaderFill = Color(0xBFFFFAF0)

    /** Glass / 衬底描边 rgba(255,255,255,0.65) */
    val GlassBorder = Color(0xA6FFFFFF)
}
