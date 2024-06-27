Using 10 Steps:

1. 對你想建立的 Package 點選右鍵移到”New”並選擇 “Add Code Gen Package”:
![1](https://github.com/oscarhuang790512/CodeGenTemplateDart/assets/155708077/769d3428-ac32-4b23-808d-dd97dd989868)

2. 輸入你的 Code Gen Package Name:
![2](https://github.com/oscarhuang790512/CodeGenTemplateDart/assets/155708077/3376ce37-937c-4890-9735-b88e81c1485d)

3. 按下 OK 後會再請你輸入 Class Name:
![3](https://github.com/oscarhuang790512/CodeGenTemplateDart/assets/155708077/9a3df582-5df7-4ce0-bfb5-40cbefb1a5f0)

4. 最後會再請你輸入 Generate 出來後的檔案的縮寫檔名是什麼：
![4](https://github.com/oscarhuang790512/CodeGenTemplateDart/assets/155708077/b6f809f6-39e6-494b-829d-28695cae960a)

5. 按下 OK 後就能在 Package 中看到完整檔案：
![5](https://github.com/oscarhuang790512/CodeGenTemplateDart/assets/155708077/ebdc1887-d036-47b1-bd7a-1cb8b12f753c)

6. 請將 annotations 跟 generate 的 pubspec.yaml 檔案各自 pub get
![6](https://github.com/oscarhuang790512/CodeGenTemplateDart/assets/155708077/d9c78201-acf1-4268-beb2-e7a2e079823e)

7. 到 Gen 出來的 Package 同一層的 pubspec.yaml 檔加入依賴之後執行 pub get：
![7](https://github.com/oscarhuang790512/CodeGenTemplateDart/assets/155708077/56bd2548-7926-4d40-b292-605b53a12a12)
![7-2](https://github.com/oscarhuang790512/CodeGenTemplateDart/assets/155708077/b6e7b55d-38cb-4dd6-83fd-c91957bd6107)
![7-3](https://github.com/oscarhuang790512/CodeGenTemplateDart/assets/155708077/3984c7c6-170d-429a-9e03-4479644f7d3a)

8. 在 Package 外要 Gen 出來的 Code 進入點建立一個 File, 內容帶 annotations 的 model:
![8-2](https://github.com/oscarhuang790512/CodeGenTemplateDart/assets/155708077/12800fd3-a143-4d3f-ab94-6c7aa1e66b4c)
![8](https://github.com/oscarhuang790512/CodeGenTemplateDart/assets/155708077/2ca29d6a-c3c7-41b7-89a6-76776cd2ed90)


9. 此時就能到 Generate 的檔案開始寫你要的東西啦～
![9](https://github.com/oscarhuang790512/CodeGenTemplateDart/assets/155708077/0ae78127-03f3-432d-a503-a6a5a9612ac9)

10. 如果要測試執行結果可以在 {Package Name} 裡面執行 build_runner 的 CLI

    <code>dart run build_runner build -d -v</code>  #執行build code並印出產生細節 log

    <code>dart run build_runner watch -d -v</code>  #執行build code並印出產生細節 log, 期間不間斷等待有改動就會執行產 Code

    <code>dart run build_runner doctor</code>       #檢查 build runner 狀態
