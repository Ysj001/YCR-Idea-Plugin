package com.ysj.idea.plugin.ycr

import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedMembersSearch
import com.intellij.util.Query
import org.jetbrains.kotlin.idea.debugger.sequence.psi.callName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtValueArgument

/*
 * 一些工具
 *
 * @author Ysj
 * Create time: 2021/7/10
 */

/** 安全的强转 */
inline fun <reified T> Any?.cast(): T? = this as? T

/**
 * 获取 build 方法的参数
 */
fun PsiElement.buildArgs(): Array<out PsiExpression>? {
    if (this !is PsiMethodCallExpression) return null
    val method = this.resolveMethod() ?: return null
    val caller = method.parent
    if (method.name != "build" || caller !is PsiClass) return null
    if (caller.name != SDK_NAME && caller.supers.find { it.name != SDK_NAME } != null) return null
    return this.argumentList.expressions
}

/**
 * 获取 build 方法的参数
 */
fun KtElement.buildArgs(): List<KtValueArgument>? {
    if (this !is KtCallExpression || callName() != "build") return null
    return valueArguments
}

/**
 * 获取 @Route 注解的全 path
 */
fun PsiMember.routePath() = getAnnotation(ANNOTATION_NAME_ROUTE)?.routePath()

/**
 * 获取 @Route 注解的全 path
 */
fun PsiAnnotation.routePath() = run {
    val path = findAttributeValue("path")
    val group = findAttributeValue("group")
    if (path !is PsiLiteralValue) return@run ""
    when {
        group !is PsiLiteralValue -> path.value
        group.value.toString().isEmpty() -> path.value
        else -> "$this/${path.value}"
    }.toString()
}

/**
 * 查找工程中所有 @Route 注解
 */
fun Project.findRouteAnnotations(): Query<PsiMember>? {
    val allScope = GlobalSearchScope.allScope(this)
    val routeAnnotation = JavaPsiFacade.getInstance(this).findClass(ANNOTATION_NAME_ROUTE, allScope)
    return AnnotatedMembersSearch.search(routeAnnotation ?: return null, allScope)
}

/**
 * 查找工程中所有 @YCRGenerated 注解
 */
fun Project.findYCRGeneratedAnnotations(): Query<PsiMember>? {
    val allScope = GlobalSearchScope.allScope(this)
    val annotation = JavaPsiFacade.getInstance(this).findClass(ANNOTATION_NAME_YCR_GENERATED, allScope)
    return AnnotatedMembersSearch.search(annotation ?: return null, allScope)
}