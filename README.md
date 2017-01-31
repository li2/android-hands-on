**《Android编程权威指南》"Android Programming - The Big Nerd Ranch Guide"** 是一本非常优秀的Android入门学习教程，全书贯穿着几个简单或复杂的App的完整开发过程，这几乎是手把手地教App开发，涉及到非常多的知识点：Activity、Fragment、ViewPager、Adapter、Service、Intent......
这个Git Repository记录的是这些App的学习过程。

原本每个App对应一个Repo，后来觉得这些把 GitHub 个人主页搞得乱糟糟的，于是想合并为一个Repo. 我是按照下文介绍的方法合并的，成功：
[Merging two, three or more git repositories keeping the log history](http://www.harecoded.com/merging-two-three-or-more-git-repositories-keeping-the-log-history-2366393)
需要注意的是，Android工程文件中包含`.project`和`.classpath`，也可能包含`.gitignore`，

    # 这条命令会忽略所有.前缀的文件，
    git mv -k * path/to/repository/
    # 所以还需要：
    git mv -k .classpath path/to/repository/
    git mv -k .project path/to/repository/
    # 否则，在合并多个repos为一个时，这几个同名的文件将会导致冲突


## Criminal Intent App

这本书的第7~12章、16~22章完成了一个功能复杂的 App 开发，对于学习**M(mode)V(view)C(controller)模式** 开发非常有益处，并且涉及非常多的主题：

- 如何创建并添加一个Fragment到Activity；
- xml：样式style、主题theme、dp/sp、布局参数layout parameter、边距和内边距margin,padding；
- 使用ListFragment显示列表（介绍了如何使用单例构建数据模型，如何抽象类、ListView是如何从ArrayAdapter获取数据并呈现视图）；
- 如何创建ArrayAdapter来管理ListView的数据；
- 如何响应ListView条目的点击事件；
- 如何定制化ListView条目的布局（默认的布局仅是一个textview）；
- 如何从Fragment中启动并把参数传递给另一个Activity；
- 如何从Activity传递参数给它托管的Fragment（直接获取Activity extra，通过fragment argument bundle）；
- 如何通知Fragment的Hosting Activity返回结果；
- 如何在Fragment内获取返回结果；
- 如何通过ViewPager托管Fragment，以实现屏幕滑动的效果；
- 如何在同一个Activity托管的两个Fragment之间传递数据；
- 如何根据屏幕大小选择布局；

li2
weiyi.just2@gmail.com
2015-09-08