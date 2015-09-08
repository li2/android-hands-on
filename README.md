《Android编程权威指南》"Android Programming - The Big Nerd Ranch Guide" 是一本非常优秀的Android入门学习教程，全书贯穿着几个简单或复杂的App的完整开发过程，这几乎是手把手地教App开发，涉及到非常多的知识点：Activity、Fragment、ViewPager、Adapter、Service、Intent......
这个Git Repository记录的是这些App的学习过程。

原本每个App对应一个Repo，后来觉得这些把git主页搞得乱糟糟的，于是想合并为一个Repo. 我是按照下文介绍的方法合并的，成功：
[Merging two, three or more git repositories keeping the log history](http://www.harecoded.com/merging-two-three-or-more-git-repositories-keeping-the-log-history-2366393)
需要注意的是，Android工程文件中包含`.project`和`.classpath`，也可能包含`.gitignore`，

    # 这条命令会忽略所有.前缀的文件，
    git mv -k * path/to/repository/
    # 所以还需要：
    git mv -k .classpath path/to/repository/
    git mv -k .project path/to/repository/
    # 否则，在合并多个repos为一个时，这几个同名的文件将会导致冲突


li2
weiyi.just2@gmail.com
2015-09-08