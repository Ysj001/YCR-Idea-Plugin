package com.ysj.idea.plugin.ycr.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.ysj.idea.plugin.ycr.PLUGIN_HELP_URL

/**
 * YCR 在主菜单 About
 *
 * @author Ysj
 * Create time: 2021/7/2
 */
class AboutAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
//        BrowserUtil.browse(PLUGIN_HELP_URL)
        val project: Project = event.getData(PlatformDataKeys.PROJECT) ?: return
        Messages.showMessageDialog(
            project,
            """
                Hello, I am glad to see you.
                This is a plugin that works with the YCR lib.
                See: 
                https://github.com/Ysj001/YCR
                $PLUGIN_HELP_URL
            """.trimMargin(),
            "About",
            Messages.getInformationIcon()
        )
    }
}