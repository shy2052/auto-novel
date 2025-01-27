package infra.web

import infra.model.WebNovelMetadata
import infra.model.WebNovelTocItem
import util.epub.EpubBook
import util.epub.Navigation
import util.epub.createEpubXhtml
import java.nio.file.Path

private const val MISSING_EPISODE_HINT = "该章节缺失。"

suspend fun makeEpubFile(
    filePath: Path,
    metadata: WebNovelMetadata,
    chapters: Map<String, ChapterWriteData>,
    jp: Boolean,
    zh: Boolean,
) {
    val language =
        if (jp) "jp"
        else "zh"
    val title =
        if (jp) metadata.titleJp
        else metadata.titleZh ?: metadata.titleJp
    val introduction =
        if (jp) metadata.introductionJp
        else metadata.introductionZh ?: metadata.introductionJp
    val getChapterTitle =
        if (jp) { item: WebNovelTocItem -> item.titleJp }
        else { item: WebNovelTocItem -> item.titleZh ?: item.titleJp }

    val epub = EpubBook()
    val identifier = "${metadata.providerId}.${metadata.novelId}"
    epub.addIdentifier(identifier, true)

    epub.addTitle(title)
    epub.addLanguage(language)
    epub.addDescription(introduction)
    epub.addNavigation(
        identifier,
        Navigation(
            language = language,
            title = title,
            items = tocToNavigationItems(metadata.toc, getChapterTitle)
        )
    )

    metadata.authors.map {
        epub.addCreator(it.name)
    }

    metadata.toc.filter { it.chapterId != null }.forEachIndexed { index, token ->
        val id = "episode${index + 1}.xhtml"
        val path = "Text/$id"
        val chapter = chapters[token.chapterId]

        val resource = createEpubXhtml(path, id, language, getChapterTitle(token)) {
            if (jp) it.appendElement("h1").appendText(token.titleJp)
            if (zh) it.appendElement("h1").appendText(token.titleZh ?: token.titleJp)

            if (chapter == null) {
                it.appendElement("p").appendText(MISSING_EPISODE_HINT)
            } else {
                chapter.missingTranslations.forEach { id ->
                    it.appendElement("p").appendText("${id}翻译缺失。")
                        .attr("style", "opacity:0.4;")
                }

                for (i in chapter.jpParagraphs.indices) {
                    if (chapter.jpParagraphs[i].isBlank()) {
                        it.appendElement("p").appendText(chapter.jpParagraphs[i])
                    } else {
                        chapter.paragraphs.forEach { pwd ->
                            if (pwd.primary) {
                                it.appendElement("p").appendText(pwd.paragraphs[i])
                            } else {
                                it.appendElement("p").appendText(pwd.paragraphs[i])
                                    .attr("style", "opacity:0.4;")
                            }
                        }
                    }
                }
            }
        }
        epub.addResource(resource, true)
    }
    epub.write(filePath)
}

private fun tocToNavigationItems(
    toc: List<WebNovelTocItem>,
    title: (WebNovelTocItem) -> String
): List<Navigation.Item> {
    var index = 0
    return toc.map {
        if (it.chapterId != null) {
            index += 1
            Navigation.Item("episode$index.xhtml", title(it))
        } else {
            Navigation.Item(null, title(it))
        }
    }
}
