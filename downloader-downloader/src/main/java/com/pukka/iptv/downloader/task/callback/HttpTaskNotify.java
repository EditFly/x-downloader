package com.pukka.iptv.downloader.task.callback;

import com.pukka.iptv.downloader.model.DownloadTask;
import com.pukka.iptv.downloader.task.process.post.FtpPostProcess;
import com.pukka.iptv.downloader.task.process.post.TaskFailedProcess;
import com.pukka.iptv.downloader.task.process.post.TaskFinishedProcess;
import com.pukka.iptv.downloader.task.process.post.TaskStartedProcess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Author: wz
 * @Date: 2021/10/30 16:11
 * @Description: ftp通知器
 */
@Component
public class HttpTaskNotify extends TaskAbsoluteNotify {
    @Autowired
    private FtpPostProcess ftpPostProcess;
    @Autowired
    private TaskFailedProcess taskFailedProcess;
    @Autowired
    private TaskStartedProcess taskStartedProcess;
    @Autowired
    private TaskFinishedProcess taskFinishedProcess;

    @Override
    public void start(DownloadTask task) {
        taskStartedProcess.handler(task, true);
    }

    @Override
    public void success(DownloadTask task) {
        ftpPostProcess.handler(task, true);
    }

    @Override
    public void failed(DownloadTask task, String msg) {
        taskFailedProcess.handler(task, true);
    }

    @Override
    public void finish(DownloadTask task) {
        taskFinishedProcess.handler(task, false);
    }
}