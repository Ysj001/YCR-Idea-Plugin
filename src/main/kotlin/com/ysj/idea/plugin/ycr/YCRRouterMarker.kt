package com.ysj.idea.plugin.ycr

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.ui.Messages
import com.intellij.pom.Navigatable
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedMembersSearch
import com.intellij.util.containers.isNullOrEmpty
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.plainContent
import java.awt.event.MouseEvent

/**
 * 用于定位 YCR 的 build("/aaa/bbb") 对应的 @Route(path = "/aaa/bbb") 的标记类
 *
 * @author Ysj
 * Create time: 2021/7/2
 */
class YCRRouterMarker : LineMarkerProviderDescriptor() {

    override fun getName() = "YCR"

    override fun getLineMarkerInfo(element: PsiElement) =
        if (element is KtElement) kotlinProcess(element) else javaProcess(element)

    private fun javaProcess(element: PsiElement): LineMarkerInfo<*>? {
        if (element !is PsiCallExpression) return null
        val method = element.resolveMethod() ?: return null
        val parent = method.parent
        if (method.name != "build" || parent !is PsiClass) return null
        if (parent.name != SDK_NAME && parent.supers.find { it.name != SDK_NAME } != null) return null
        val args = element.argumentList?.expressions
        if (args.isNullOrEmpty()) return null
        val routePath = args[0] as? PsiLiteralValue ?: return null
        searchTarget(element, routePath.value.toString()) ?: return null
        return markerInfo(element) { _, elm ->
            // GutterIconNavigationHandler 这玩意会复用？所以重新 searchTarget
            searchTarget(elm, routePath.value.toString())?.navigate(true)
        }
    }

    private fun kotlinProcess(element: KtElement): LineMarkerInfo<*>? {
        if (element !is KtNameReferenceExpression) return null
        if (element.getReferencedName() != "build") return null
        val parent = (element as PsiElement).parent
        if (parent !is KtCallExpression) return null
        val args = parent.valueArguments
        if (args.isNullOrEmpty()) return null
        val routePath = (args[0].getArgumentExpression() as? KtStringTemplateExpression)?.plainContent ?: return null
        searchTarget(element, routePath) ?: return null
        return markerInfo(element) { _, elm ->
            searchTarget(elm, routePath)?.navigate(true)
        }
    }

    private fun searchTarget(element: PsiElement, routePath: String): Navigatable? {
        val project = element.project
        val allScope = GlobalSearchScope.allScope(project)
        val routeAnnotation = JavaPsiFacade.getInstance(project).findClass(ROUTE_ANNOTATION_NAME, allScope)
        return AnnotatedMembersSearch.search(routeAnnotation ?: return null, allScope)
            .findAll()
            .find { mb ->
                routePath in mb.annotations.mapNotNull { ann ->
                    val path = ann.findAttributeValue("path")
                    val group = ann.findAttributeValue("group")
                    if (path !is PsiLiteralValue || group !is PsiLiteralValue) return@mapNotNull null
                    group.value.toString().run { if (isEmpty()) path.value else "$this/${path.value}" }
                }
            }
    }

    private fun markerInfo(
        element: PsiElement,
        navHandler: GutterIconNavigationHandler<PsiElement>,
    ) = LineMarkerInfo(
        element,
        element.textRange,
        AllIcons.General.Locate,
        null,
        navHandler,
        GutterIconRenderer.Alignment.LEFT,
    )

}