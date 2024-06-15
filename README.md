# note
A multi-function Notepad APP on the Android operating system. 

安卓多功能记事本

文档相关: https://juejin.cn/post/6975504592494657566
http://t.csdnimg.cn/CE7Vs

视频演示：https://www.bilibili.com/video/BV1sz4y1D7a6/

本软件结合了目前先进的语音技术和云存储技术，通过语音识别技术可以快速记录用户所萌生的想法，同时结合云服务，可以同步到云端，不容易造成数据丢失，做到本地和云端双重保险。此外，该软件还支持富文本功能，可以很好提升文字的辨识度，便于区分正文、标题等等。同时，也支持图片的插入和保存，方便用户以后查看笔记时，保存所需要的图片，不必要再到相册花费大量时间去寻找。最后，该软件同时支持应用内提醒和日历提醒，前者可以实现语音播报提醒，非常方便用户快捷地知道提醒内容，后者依靠系统日历来提醒，不需要把软件运行在后台，可以降低手机性能功耗。
该软件实现了对想法的记录，包括文本、语音和图片，主打良好的用户体验，语音快捷输入和应用内外提醒功能，让用户能够随时记录下他们认为重要的信息。

This software combines the current advanced voice technology and cloud storage technology, through voice recognition technology which can quickly record the user's ideas. Combining with cloud services, the data of the software can be synchronized to the cloud, so it is not easy to cause data loss, to achieve local data and cloud data double insurance. In addition, the software also supports rich text function, which can improve the recognition of text and facilitate the distinction between text and title. At the same time, it also supports the insertion and storage of pictures, which is convenient for users to save the pictures they need when viewing notes in the future, without having to spend a lot of time to find photos in the album. Finally, the software supports both in-app reminder and calendar reminder. The former can implement voice broadcast reminder, which is very convenient for users to quickly know the reminder content. The latter relies on the system calendar to remind, without having to run the software in the background, which can reduce mobile phone performance consumption.

## 系统架构

![image-20200712182511754](C:\Users\wesle\AppData\Roaming\Typora\typora-user-images\image-20200712182511754.png)

各个系统模块功能如下：

（1） 用户界面：与用户直接打交道的就是用户界面，所以一个视觉美观和操作便捷的用户界面至关重要，它直接影响了用户体验。

（2） 输入模块：主要用来记录用户想法，包括文字、图片和语音。其中，文字输入可以通过传统的键盘输入或者语音识别输入。

（3） 显示模块：用来显示保存后的笔记或者待办事项。

（4） 提醒模块：目前，人们行色匆匆，需要忙活的事情太多了。所以，记事本的提醒功能必不可少，包括日历提醒和应用内语音播报提醒。

（5） 数据同步：现在是大数据时代，很多本地数据都支持同步到云端了。因此，用户的数据可以同时保存到本地和云端，便捷性和安全性都得到了提高。

（6） 搜索模块：用户的数据量是巨大的，有了搜索功能才能在海量的信息中找到自己需要的，节省大量查找时间。

（7） 管理模块：主要就是负责笔记和待办事项的增删改查。此外，为了避免误删除笔记，系统还提供回收站功能，当你后悔的时候，还可以从回收站恢复笔记内容。由于待办事项是一些简短的文字记录，办完事后基本没用了，所以只有永久删除选项。此外，还可以更改笔记分组。

（8） 分享模块：现在是互联网时代，人与人之间的交流变得越来越容易。用户可将笔记通过社交软件实现与他人的便捷分享，让笔记不单单是自己手机上的简单存储。[9]这样就可以节省大量的复制粘贴时间，通过分享接口直接发送消息。

（9） 权限申请模块：安卓6.0系统开始引入了运行时权限功能，对于一些危险的权限，例如录音，读取数据等等，需要得到用户的授权。对于普通权限，比如读取网络信息，由系统自动完成授权。



**请在编译该项目前在MainActivity.java文件中添加自己申请到的讯飞应用密钥。**

```
//初始化讯飞语音
SpeechUtility.createUtility(this, SpeechConstant.APPID +"=");  
//请在冒号里面添加讯飞开发者网站申请的密钥
```

最后，感谢**[ StarNote](https://github.com/StarDxxx/StarNote)** 开源项目，本安卓记事本借鉴了其内容。
