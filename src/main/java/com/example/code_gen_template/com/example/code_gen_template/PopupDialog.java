package com.example.code_gen_template.com.example.code_gen_template;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.command.WriteCommandAction;

import java.io.File;
import java.io.IOException;

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
        String[] scriptPath = (directory + "/" + packageName + "/auto_execute_flow.sh").split(":");

        if (packageName != null && !packageName.trim().isEmpty()) {
            try {
                createPackage(project, directory, packageName, className, extensionName);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            Messages.showInfoMessage("Package created successfully", "Success");
//            int result = Messages.showOkCancelDialog("Need auto run pub get and auto dependencies package?", "Confirm Box", "OK", "Cancel", Messages.getQuestionIcon());
//            if (result == Messages.OK) {
//                WriteCommandAction.runWriteCommandAction(project, () -> {
//                    try {
//                        // 检查文件是否存在
//                        File file = new File(scriptPath[1]);
//                        if (file.exists()) {
//                            // 构建 ProcessBuilder 对象
//                            ProcessBuilder processBuilder = new ProcessBuilder("sh", scriptPath[1]);
//
//                            // 启动进程
//                            Process process = processBuilder.start();
//
//                            // 等待进程执行完成
//                            int exitCode = process.waitFor();
//
//                            // 打印进程执行结果
//                            if (exitCode == 0) {
//                                Messages.showInfoMessage("Shell script execute success", "Success");
//                            } else {
//                                Messages.showErrorDialog("Shell script execute failed, error code：" + exitCode, "Error");
//                            }
//                        } else {
//                            Messages.showInfoMessage("文件不存在：" + scriptPath[1], "Fail");
//                        }
//                    } catch (IOException | InterruptedException iex) {
//                        Messages.showErrorDialog(iex.getMessage(), "Error");
//                        iex.printStackTrace();
//                    }
//                });
//            }
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
            try {
                createBashForAutomaticExecFlow(currentDirectory);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }


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

    private void createBashForAutomaticExecFlow(PsiDirectory packageDirectory) throws IOException {
        packageDirectory.createFile("auto_execute_flow.sh").getVirtualFile().setBinaryContent(
                ("#!/bin/bash\n" +
                        "source ~/.bash_profile\n" +
                        "source ~/.zshrc\n" +
                        "base_path=\"$(dirname \"$(pwd)\")\"\n" +
                        "base_path_name=\"$(basename \"$(pwd)\")\"\n" +
                        "local_path=\"$(pwd)\"\n" +
                        "annotations_path_name=\"$(find . -maxdepth 1 -type d -name \"*annotations\" | awk -F/ '{print $2}')\"\n" +
                        "generate_path_name=\"$(find . -maxdepth 1 -type d -name \"*generate\" | awk -F/ '{print $2}')\"\n" +
                        "\n" +
                        "find_pubspec() {\n" +
                        "    local dir=\"$1\"\n" +
                        "\n" +
                        "    # Check if pubspec.yaml exists in the current directory\n" +
                        "    if [ -f \"$dir/pubspec.yaml\" ]; then\n" +
                        "        echo \"$dir\"\n" +
                        "        return 0\n" +
                        "    fi\n" +
                        "\n" +
                        "    # If we are at the root directory, stop searching\n" +
                        "    if [ \"$dir\" = \"/\" ]; then\n" +
                        "        return 1\n" +
                        "    fi\n" +
                        "\n" +
                        "    # Move to the parent directory and continue searching\n" +
                        "    find_pubspec \"$(dirname \"$dir\")\"\n" +
                        "}\n" +
                        "\n" +
                        "result=$(find_pubspec $local_path)\n" +
                        "\n" +
                        "run_pub_get() {\n" +
                        "  cd $local_path/$annotations_path_name\n" +
                        "  flutter pub get\n" +
                        "  cd $local_path/$generate_path_name\n" +
                        "  flutter pub get\n" +
                        "}\n" +
                        "\n" +
                        "add_dependencies_in_yaml(){\n" +
                        "  cd $result\n" +
                        "  FOUND_SOURCE_GEN=0\n" +
                        "  FOUND_CODE_BUILDER=0\n" +
                        "  FOUND_BUILD_RUNNER=0\n" +
                        "  FOUND_ANNOTATIONS=0\n" +
                        "  FOUND_GENERATE=0\n" +
                        "  if grep -q 'source_gen:' pubspec.yaml; then\n" +
                        "      FOUND_SOURCE_GEN=1\n" +
                        "  fi\n" +
                        "  if grep -q 'code_builder:' pubspec.yaml; then\n" +
                        "      FOUND_CODE_BUILDER=1\n" +
                        "  fi\n" +
                        "  if grep -q 'build_runner:' pubspec.yaml; then\n" +
                        "      FOUND_BUILD_RUNNER=1\n" +
                        "  fi\n" +
                        "  if grep -q $annotations_path_name pubspec.yaml; then\n" +
                        "      FOUND_ANNOTATIONS=1\n" +
                        "  fi\n" +
                        "  if grep -q $generate_path_name pubspec.yaml; then\n" +
                        "      FOUND_GENERATE=1\n" +
                        "  fi\n" +
                        "  find . -maxdepth 1 -type f -name \"pubspec.yaml\" | while read -r file; do\n" +
                        "      awk '\n" +
                        "      {\n" +
                        "        print\n" +
                        "      }\n" +
                        "      /^dependencies:$/ {\n" +
                        "          if('$FOUND_SOURCE_GEN' == 0){\n" +
                        "            print \"  source_gen: ^1.5.0\"\n" +
                        "          }\n" +
                        "          if('$FOUND_CODE_BUILDER' == 0){\n" +
                        "            print \"  code_builder: ^4.10.0\"\n" +
                        "          }\n" +
                        "          if('$FOUND_ANNOTATIONS' == 0){\n" +
                        "            print \"  '$annotations_path_name': \"\n" +
                        "            print \"    path: '$base_path_name'/'$annotations_path_name'\"\n" +
                        "          }\n" +
                        "      }\n" +
                        "      /^dev_dependencies:$/ {\n" +
                        "          if('$FOUND_BUILD_RUNNER' == 0){\n" +
                        "            print \"  build_runner: ^2.4.8\"\n" +
                        "          }\n" +
                        "          if('$FOUND_GENERATE' == 0){\n" +
                        "            print \"  '$generate_path_name': \"\n" +
                        "            print \"    path: '$base_path_name'/'$generate_path_name'\"\n" +
                        "          }\n" +
                        "      }\n" +
                        "      ' \"$file\" > temp_file && mv temp_file \"$file\"\n" +
                        "  done\n" +
                        "  flutter pub get\n" +
                        "}\n" +
                        "\n" +
                        "run_pub_get\n" +
                        "if [ -n \"$result\" ]; then\n" +
                        "    echo \"pubspec.yaml found in: $result\"\n" +
                        "    add_dependencies_in_yaml\n" +
                        "else\n" +
                        "    echo \"pubspec.yaml not found\"\n" +
                        "fi\n").getBytes()
        );
    }
}
