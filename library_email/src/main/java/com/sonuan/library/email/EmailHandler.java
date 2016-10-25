package com.sonuan.library.email;

import android.os.AsyncTask;
import android.util.Log;

import com.sun.mail.util.MailSSLSocketFactory;

import java.io.File;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.Properties;

import javax.activation.CommandMap;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.activation.MailcapCommandMap;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * 邮件发送工具，支持添加附件
 * 
 * @author kycq
 * 
 */
public class EmailHandler extends AsyncTask<String, Object, String> {
	private static final String TAG = EmailHandler.class.getSimpleName();

	private static final int PROGRESS_START = 1;
	private static final int PROGRESS_SUCCESS = 2;
	private static final int PROGRESS_FAILURE = 3;

	private Properties mProperties;
	private Session mSession;
	private Message mMessage;
	private MimeMultipart mMultipart;

	private OnProgressListener mProgressListener;

	public EmailHandler() {
		mProperties = new Properties();
	}

	public EmailHandler(OnProgressListener listener) {
		mProperties = new Properties();
		setOnProgressListener(listener);
	}

	/**
	 * 设置邮件发送监听器
	 * 
	 * @param listener
	 *            邮件监听器
	 */
	public void setOnProgressListener(OnProgressListener listener) {
		mProgressListener = listener;
	}

	/**
	 * 配置属性
	 * 
	 * @param host
	 *            邮件服务器地址
	 * @param post
	 *            邮件服务器端口
	 */
	public void setProperties(String host, String post, boolean isSSL) {
		// 地址
		mProperties.put("mail.smtp.host", host);
		// 端口号
		mProperties.put("mail.smtp.post", post);
		// 是否验证
		mProperties.put("mail.smtp.auth", "true");

		mProperties.put("mail.transport.protocol", "smtp");
		mSession = Session.getInstance(mProperties);
		mMessage = new MimeMessage(mSession);
		mMultipart = new MimeMultipart();
		if (isSSL) {
			try {
				MailSSLSocketFactory sf = new MailSSLSocketFactory();
				sf.setTrustAllHosts(true);
				mProperties.put("mail.smtp.starttls.enable", "true");
				mProperties.put("mail.smtp.ssl.enable", "true");
				mProperties.put("mail.smtp.ssl.socketFactory", sf);
			} catch (GeneralSecurityException e) {
				e.printStackTrace();
			}
		}
	}

	public void setDebug(boolean debug) {
		if (debug) {
			mProperties.put("mail.debug", "true");
		}
	}

	/**
	 * 设置收件人
	 * 
	 * @param receivers
	 *            收件人地址
	 * @throws MessagingException
	 *             邮件错误信息
	 */
	public void setReceivers(String... receivers) throws MessagingException {
		Address[] address = new InternetAddress[receivers.length];
		for (int i = 0; i < receivers.length; i++) {
			address[i] = new InternetAddress(receivers[i]);
		}
		mMessage.setRecipients(Message.RecipientType.TO, address);
	}

	/**
	 * 设置邮件格式信息
	 * 
	 * @param from
	 *            邮件来源
	 * @param subject
	 *            邮件主题
	 * @param content
	 *            邮件内容
	 * @throws AddressException
	 *             地址错误信息
	 * @throws MessagingException
	 *             邮件错误信息
	 */
	public void setMessage(String from, String subject, String content)
			throws AddressException, MessagingException {
		mMessage.setFrom(new InternetAddress(from));
		mMessage.setSubject(subject);
		MimeBodyPart textBody = new MimeBodyPart();
		textBody.setContent(content, "text/html;charset=gbk");
		mMultipart.addBodyPart(textBody);
	}

	/**
	 * 添加附件
	 * 
	 * @param filePath
	 *            附件文件路径
	 * @throws MessagingException
	 *             邮件错误信息
	 */
	public void addAttachment(String filePath) throws MessagingException {
		FileDataSource fileDataSource = new FileDataSource(new File(filePath));
		DataHandler dataHandler = new DataHandler(fileDataSource);
		MimeBodyPart mimeBodyPart = new MimeBodyPart();
		mimeBodyPart.setDataHandler(dataHandler);
		mimeBodyPart.setFileName(fileDataSource.getName());
		mMultipart.addBodyPart(mimeBodyPart);
	}

	/**
	 * 启动异步线程发送邮件
	 * 
	 * @param host
	 *            邮件发送服务器
	 * @param account
	 *            邮箱帐号
	 * @param pwd
	 *            邮箱密码
	 */
	public void sendEmail(String host, String account, String pwd) {
		execute(host, account, pwd);
	}

	@Override
	protected String doInBackground(String... params) {
		try {
			publishProgress(PROGRESS_START);

			MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
			mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
			mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
			mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
			mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
			mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822");
			CommandMap.setDefaultCommandMap(mc);

			// 发送时间
			mMessage.setSentDate(new Date());
			// 发送的内容，文本和附件
			mMessage.setContent(mMultipart);
			mMessage.saveChanges();
			// 创建邮件发送对象，并指定其使用SMTP协议发送邮件
			Transport transport = mSession.getTransport("smtp");
			// 登录邮箱
			transport.connect((String) params[0], (String) params[1],
					(String) params[2]);
			// 发送邮件
			transport.sendMessage(mMessage, mMessage.getAllRecipients());
			// 关闭连接
			transport.close();

			Log.i(TAG, "doInBackground # success");
			publishProgress(PROGRESS_SUCCESS, "邮件发送成功！");
		} catch (Exception e) {
			Log.e(TAG, "doInBackground # failure", e);
			publishProgress(PROGRESS_FAILURE, e, "邮件发送失败！");
		}
		return "邮件发送成功！";
	}

	@Override
	protected void onProgressUpdate(Object... values) {
		if (mProgressListener == null) {
			return;
		}
		int what = (Integer) values[0];

		switch (what) {
		case PROGRESS_START:// 开始
			mProgressListener.onStart();
			break;
		case PROGRESS_SUCCESS:// 成功
			mProgressListener.onSuccess();
			break;
		case PROGRESS_FAILURE:// 失败
			mProgressListener.onFailure();
			break;
		}
	}

	/**
	 * 邮件发送状态监听器
	 * 
	 * @author kycq
	 * 
	 */
	public static interface OnProgressListener {
		/** 邮件发送开始 */
		public void onStart();

		/** 邮件发送成功 */
		public void onSuccess();

		/** 邮件发送失败 */
		public void onFailure();
	}
}
