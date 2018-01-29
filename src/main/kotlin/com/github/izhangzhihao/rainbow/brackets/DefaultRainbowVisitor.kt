package com.github.izhangzhihao.rainbow.brackets

import com.intellij.codeInsight.daemon.impl.HighlightVisitor
import com.intellij.lang.BracePair
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.tree.IElementType

/**
 * DefaultRainbowVisitor
 *
 * Created by Yii.Guxing on 2018/1/23
 */
class DefaultRainbowVisitor : RainbowHighlightVisitor() {

    override fun clone(): HighlightVisitor = DefaultRainbowVisitor()

    override fun visit(element: PsiElement) {
        val type = (element as? LeafPsiElement)?.elementType ?: return
        val pairs = element.language.bracePairs ?: return
        val pair = pairs.find { it.leftBraceType == type || it.rightBraceType == type } ?: return

        val level = element.getBracketLevel(pair)
        if (level >= 0) {
            element.setHighlightInfo(level)
        }
    }

    companion object {
        private fun LeafPsiElement.getBracketLevel(pair: BracePair)
                : Int = if (isValidBracket(pair)) iterateBracketParents(parent, pair, -1) else -1

        private tailrec fun iterateBracketParents(element: PsiElement?, pair: BracePair, count: Int): Int {
            if (element == null || element is PsiFile) {
                return count
            }

            var nextCount = count
            if (element.haveBrackets(pair)) {
                nextCount++
            }

            return iterateBracketParents(element.parent, pair, nextCount)
        }

        private fun PsiElement.haveBrackets(pair: BracePair): Boolean {
            if (this is LeafPsiElement) {
                return false
            }

            val leftBraceType = pair.leftBraceType
            val rightBraceType = pair.rightBraceType
            var findLeftBracket = false
            var findRightBracket = false
            var left: PsiElement? = firstChild
            var right: PsiElement? = lastChild
            while (left != right && (!findLeftBracket || !findRightBracket)) {
                val needBreak = left == null || left.nextSibling == right

                if (left is LeafPsiElement && left.elementType == leftBraceType) {
                    findLeftBracket = true
                } else {
                    left = left?.nextSibling
                }
                if (right is LeafPsiElement && right.elementType == rightBraceType) {
                    findRightBracket = true
                } else {
                    right = right?.prevSibling
                }

                if (needBreak) {
                    break
                }
            }

            return findLeftBracket && findRightBracket
        }

        private fun LeafPsiElement.isValidBracket(pair: BracePair): Boolean {
            val pairType = when (elementType) {
                pair.leftBraceType -> pair.rightBraceType
                pair.rightBraceType -> pair.leftBraceType
                else -> return false
            }

            return if (pairType == pair.leftBraceType) {
                checkBracePair(this, parent.firstChild, pairType, PsiElement::getNextSibling)
            } else {
                checkBracePair(this, parent.lastChild, pairType, PsiElement::getPrevSibling)
            }
        }

        private inline fun checkBracePair(brace: PsiElement,
                                          start: PsiElement,
                                          type: IElementType,
                                          next: PsiElement.() -> PsiElement?): Boolean {
            var element: PsiElement? = start
            while (element != null && element != brace) {
                if (element is LeafPsiElement && element.elementType == type) {
                    return true
                }

                element = element.next()
            }

            return false
        }
    }
}