package com.ysj.idea.plugin.ycr.actions

import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.contextOfType
import com.intellij.util.Processor
import com.ysj.idea.plugin.ycr.*

private var PACKAGE_NAME: String = ""

private const val CLASS_NAME = "YCRConst"

/**
 * 生成 YCR 相关常量
 *
 * @author Ysj
 * Create time: 2021/7/12
 */
class GenerateConst : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
//        val editor = CommonDataKeys.EDITOR.getData(e.dataContext) ?: return
        val ideView = e.getData(LangDataKeys.IDE_VIEW) ?: return
        val dir = ideView.orChooseDirectory ?: return
        val jpf = JavaPsiFacade.getInstance(project)
        val factory = jpf.elementFactory
        project.generateYCRConst(dir, factory)
    }

}

/**
 * 刷新 YCR 相关常量
 *
 * @author Ysj
 * Create time: 2021/7/14
 */
class RefreshConstFile : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val findYCRConst = project.findYCRConst()
        if (findYCRConst == null) {
            Messages.showMessageDialog(project, "Not found const file!", "Warning", Messages.getWarningIcon())
            return
        }
        project.generateYCRConst(
            findYCRConst.containingFile.containingDirectory ?: return,
            JavaPsiFacade.getInstance(project).elementFactory
        )
    }
}

private fun Project.generateYCRConst(
    dir: PsiDirectory,
    factory: PsiElementFactory,
) = WriteCommandAction.writeCommandAction(this).run<Throwable> {
    val psiPackage = JavaDirectoryService.getInstance().getPackage(dir) ?: return@run
    var psiFile = dir.findFile("$CLASS_NAME.java")
    psiFile = if (psiFile == null) dir.createFile("$CLASS_NAME.java") else {
        psiFile.delete()
        dir.createFile("$CLASS_NAME.java")
    }
    psiFile.add(factory.createCommentFromText("// Generated code from YCR-Idea-Plugin", psiFile))
    psiFile.add(factory.createPackageStatement(psiPackage.qualifiedName))
    val ycrConstClass = psiFile.add(factory.createClass(CLASS_NAME)) as PsiClass
    ycrConstClass.modifierList?.apply {
        setModifierProperty("public", true)
        setModifierProperty("final", true)
    }
    psiFile.addBefore(factory.createAnnotationFromText("@$ANNOTATION_NAME_YCR_GENERATED", ycrConstClass), ycrConstClass)
    ycrConstClass.navigate(true)
    val routeClass = ycrConstClass.add(factory.createClass("route")) as PsiClass
    routeClass.modifierList?.apply {
        setModifierProperty("final", true)
        setModifierProperty("static", true)
    }
    findRouteAnnotations()?.allowParallelProcessing()?.forEach(Processor { pm ->
        val annotation = pm.getAnnotation(ANNOTATION_NAME_ROUTE) ?: return@Processor false
        val routePath = annotation.routePath()
        val fieldName = routePath.substring(1).replace("/", "_")
        val fieldText = "public static final String $fieldName = \"$routePath\";"
        val filed = routeClass.add(factory.createFieldFromText(fieldText, routeClass))
        val target = annotation.contextOfType(PsiClass::class)?.qualifiedName ?: ""
        routeClass.addBefore(factory.createDocCommentFromText("/** {@linkplain $target} */"), filed)
        true
    })
    // 格式化代码
//        val styleManager = JavaCodeStyleManager.getInstance(this)
    ReformatCodeProcessor(psiFile, false).runWithoutProgress()
    PACKAGE_NAME = psiPackage.qualifiedName
}

/**
 * 通过 [ANNOTATION_NAME_YCR_GENERATED] 注解查找已经生成的 YCRConst 类
 */
private fun Project.findYCRConst() = findYCRGeneratedAnnotations()?.run {
    forEach { pm ->
        pm.getAnnotation(ANNOTATION_NAME_YCR_GENERATED)
            ?.contextOfType(PsiClass::class)
            ?.also { if (CLASS_NAME == it.name) return@run it }
    }
    null
}