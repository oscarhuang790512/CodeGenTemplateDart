package com.example.code_gen_template.com.example.code_gen_template;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.command.WriteCommandAction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PopupDialog extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // 獲取當前的項目和目錄
        Project project = e.getData(PlatformDataKeys.PROJECT);
        PsiDirectory directory = e.getData(PlatformDataKeys.PSI_ELEMENT) instanceof PsiDirectory
                ? (PsiDirectory) e.getData(PlatformDataKeys.PSI_ELEMENT)
                : null;

        if (project == null || directory == null) {
            Messages.showErrorDialog("No directory selected", "Error");
            return;
        }

        // 請求用戶輸入包名稱
        String packageName = Messages.showInputDialog(project, "Enter package name:", "Code Generate Package", Messages.getQuestionIcon());
        String className = Messages.showInputDialog(project, "Enter class name:", "Code Generate Package", Messages.getQuestionIcon());
        String extensionName = Messages.showInputDialog(project, "Enter gen file Extension name:", "Code Generate Package", Messages.getQuestionIcon());

        if (packageName != null && !packageName.trim().isEmpty()) {
            try {
                createPackage(project, directory, packageName, className, extensionName);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            Messages.showInfoMessage("Package created successfully", "Success");
        }
    }

    private void createPackage(Project project, PsiDirectory directory, String packageName, String className, String extensionName) throws IOException {
        WriteCommandAction.runWriteCommandAction(project, () -> {
            String[] packageParts = packageName.split("\\.");
            PsiDirectory currentDirectory = directory;

            for (String part : packageParts) {
                PsiDirectory subDirectory = currentDirectory.findSubdirectory(part);
                if (subDirectory == null) {
                    subDirectory = currentDirectory.createSubdirectory(part);
                }
                currentDirectory = subDirectory;
            }

            // 創建 Flutter 子包
            try {
                createFlutterPackage(currentDirectory, String.format("%s_annotations", packageName), className, extensionName, packageName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                createFlutterPackage(currentDirectory, String.format("%s_generate", packageName), className, extensionName, packageName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
//            // 选择当前项目内的 pubspec.yaml 文件
//            PsiFile pubspecFile = choosePubspecFile(project);
//            if (pubspecFile == null) {
//                return;
//            }
//
//            // 在 dependencies 后插入新行
//            insertDependencyLine(pubspecFile);
        });
    }

//    private PsiFile choosePubspecFile(Project project) {
//        VirtualFile[] contentRoots = project.getBaseDir().getChildren();
//        for (VirtualFile contentRoot : contentRoots) {
//            VirtualFile pubspecFile = contentRoot.findFileByRelativePath("pubspec.yaml");
//            if (pubspecFile != null && pubspecFile.exists()) {
//                return PsiManager.getInstance(project).findFile(pubspecFile);
//            }
//        }
//        return null;
//    }
//
//    private void insertDependencyLine(PsiFile pubspecFile) {
//        // 找到 dependencies 关键字后的位置插入新行
//        PsiElement[] children = pubspecFile.getChildren();
//        for (PsiElement child : children) {
//            if (child.getText().contains("dependencies")) {
//                PsiElement parent = child.getParent();
//                if (parent != null) {
//                    PsiElement nextSibling = parent.getNextSibling();
//                    if (nextSibling != null) {
//                        String newLineText = "  new_dependency: ^1.0.0"; // 要插入的新行文本，替换为实际需要插入的内容
//                        int insertionOffset = nextSibling.getTextOffset();
//
//                        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(pubspecFile.getProject());
//                        documentManager.getDocument(pubspecFile).insertString(insertionOffset, "\n" + newLineText);
//                        documentManager.commitDocument(documentManager.getDocument(pubspecFile));
//                        return;
//                    }
//                }
//            }
//        }
//    }


    private void createFlutterPackage(PsiDirectory parentDirectory, String directoryName, String className, String extensionName, String packageName) throws IOException {
        PsiDirectory packageDirectory = parentDirectory.findSubdirectory(directoryName);
        if (packageDirectory == null) {
            packageDirectory = parentDirectory.createSubdirectory(directoryName);
        }

        // 创建必要的目录和文件
        PsiDirectory libDirectory = packageDirectory.createSubdirectory("lib");
        libDirectory.createFile(String.format("%s.dart", directoryName)).getVirtualFile().setBinaryContent(String.format("library %s;", directoryName).getBytes());

        if (directoryName.contains("annotations")) {
            createAnnotationsYaml(packageDirectory, directoryName);
            createAnnotationsModel(libDirectory, className);
        }
        if (directoryName.contains("generate")) {
            createGenerateDirectory(libDirectory, directoryName, className, packageName);
            createGenerateYaml(packageDirectory, packageName, directoryName);
            createBuildYaml(packageDirectory, packageName, extensionName);
            createGenerateHelpBuilder(libDirectory, className, extensionName, packageName);
        }
    }

    private void createGenerateDirectory(PsiDirectory parentDirectory, String directoryName, String className, String packageName) throws IOException {
        String DirectoryName = "generate_main_code";
        PsiDirectory packageDirectory = parentDirectory.findSubdirectory(DirectoryName);
        if (packageDirectory == null) {
            packageDirectory = parentDirectory.createSubdirectory(DirectoryName);
        }

        // 创建必要的目录和文件
        packageDirectory.createFile(String.format("%s.dart", "generate_" + packageName)).getVirtualFile().setBinaryContent(
                String.format("import 'package:analyzer/dart/element/element.dart';\n" +
                        "import 'package:%s_annotations/generate_model.dart';\n" +
                        "import 'package:build/src/builder/build_step.dart';\n" +
                        "import 'package:source_gen/source_gen.dart';\n" +
                        "import 'package:build/build.dart';\n" +
                        "import 'package:code_builder/code_builder.dart';\n" +
                        "import 'package:dart_style/dart_style.dart';\n" +
                        "import 'dart:io';\n" +
                        "\n" +
                        "class Generate%s\n" +
                        "    extends GeneratorForAnnotation<Generate%sModel> {\n" +
                        "  @override\n" +
                        "  generateForAnnotatedElement(\n" +
                        "      Element element, ConstantReader annotation, BuildStep buildStep) async {\n" +
                        "   /// TODO: Write Your Gen Code Here\n" +
                        "   return \"class %s {}\";" +
                        "  }\n" +
                        "}", packageName, className, className, className).getBytes()
        );
    }

    private void createAnnotationsYaml(PsiDirectory packageDirectory, String packageName) throws IOException {
        packageDirectory.createFile("pubspec.yaml").getVirtualFile().setBinaryContent(
                ("name: " + packageName + "\n" +
                        "description: A new Flutter package.\n" +
                        "version: 0.0.1\n" +
                        "publish_to: none\n" +
                        "environment:\n" +
                        "  sdk: \">=3.3.0 <4.0.0\"\n" +
                        "  flutter: \">=1.17.0\"\n\n" +
                        "dependencies:\n" +
                        "  flutter:\n" +
                        "    sdk: flutter\n\n" +
                        "dev_dependencies:\n" +
                        "  flutter_test:\n" +
                        "    sdk: flutter\n\n" +
                        "  flutter_lints: ^3.0.0\n" +
                        "flutter:").getBytes()
        );
    }

    private void createAnnotationsModel(PsiDirectory packageDirectory, String className) throws IOException {
        packageDirectory.createFile("generate_model.dart").getVirtualFile().setBinaryContent(
                (String.format("class Generate%sModel {\n" +
                        "  final String generateName;\n" +
                        "\n" +
                        "  const Generate%sModel(this.generateName);\n" +
                        "}\n", className, className)).getBytes()
        );
    }

    private void createGenerateYaml(PsiDirectory packageDirectory, String packageName, String directoryName) throws IOException {
        packageDirectory.createFile("pubspec.yaml").getVirtualFile().setBinaryContent(
                (String.format("name: " + directoryName + "\n" +
                        "description: A new Flutter package.\n" +
                        "version: 0.0.1\n" +
                        "publish_to: none\n" +
                        "environment:\n" +
                        "  sdk: \">=3.3.0 <4.0.0\"\n" +
                        "  flutter: \">=1.17.0\"\n\n" +
                        "dependencies:\n" +
                        "  flutter:\n" +
                        "    sdk: flutter\n\n" +
                        "  %s_annotations:\n" +
                        "    path: ../%s_annotations\n" +
                        "  source_gen: ^1.5.0\n\n" +
                        "dev_dependencies:\n" +
                        "  flutter_test:\n" +
                        "    sdk: flutter\n" +
                        "  build_runner: ^2.4.8\n" +
                        "  flutter_lints: ^3.0.0\n\n" +
                        "flutter:", packageName, packageName)).getBytes()
        );
    }

    private void createBuildYaml(PsiDirectory packageDirectory, String packageName, String extensionName) throws IOException {
        packageDirectory.createFile("build.yaml").getVirtualFile().setBinaryContent(
                (String.format("builders:\n" +
                        "  generate_help_builder:\n" +
                        "    target: '%s_annotations'\n" +
                        "    import: 'package:%s_generate/generate_help_builder.dart'\n" +
                        "    builder_factories: [ 'nativeCallGenerateBuilder' ]\n" +
                        "    build_extensions: { '.dart': [ '.%s.g.dart' ] }\n" +
                        "    auto_apply: dependents\n" +
                        "    build_to: source\n" +
                        "    applies_builders: [ \"source_gen|combining_builder\" ]\n", packageName, packageName, extensionName)).getBytes()
        );
    }

    private void createGenerateHelpBuilder(PsiDirectory packageDirectory, String className, String extensionName, String packageName) throws IOException {
        packageDirectory.createFile("generate_help_builder.dart").getVirtualFile().setBinaryContent(
                (String.format("import 'package:build/build.dart';\n" +
                        "import 'generate_main_code/generate_%s.dart';\n" +
                        "import 'package:source_gen/source_gen.dart';\n" +
                        "\n" +
                        "Builder nativeCallGenerateBuilder(BuilderOptions options) =>\n" +
                        "    LibraryBuilder(Generate%s(), generatedExtension: '.%s.g.dart');\n", packageName, className, extensionName)).getBytes()
        );
    }
}
