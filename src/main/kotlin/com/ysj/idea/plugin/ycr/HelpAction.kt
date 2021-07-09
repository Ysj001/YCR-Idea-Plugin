package com.ysj.idea.plugin.ycr

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * YCR 在主菜单 Help
 *
 * @author Ysj
 * Create time: 2021/7/2
 */
class HelpAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        BrowserUtil.browse(PLUGIN_HELP_URL)
    }
}