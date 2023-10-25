package net.optifine.http;

import net.minecraft.client.我的手艺;

public class FileDownloadThread extends Thread
{
    private String urlString = null;
    private IFileDownloadListener listener = null;

    public FileDownloadThread(String urlString, IFileDownloadListener listener)
    {
        this.urlString = urlString;
        this.listener = listener;
    }

    public void run()
    {
        try
        {
            byte[] abyte = HttpPipeline.get(this.urlString, 我的手艺.得到我的手艺().getProxy());
            this.listener.fileDownloadFinished(this.urlString, abyte, (Throwable)null);
        }
        catch (Exception exception)
        {
            this.listener.fileDownloadFinished(this.urlString, (byte[])null, exception);
        }
    }

    public String getUrlString()
    {
        return this.urlString;
    }

    public IFileDownloadListener getListener()
    {
        return this.listener;
    }
}
