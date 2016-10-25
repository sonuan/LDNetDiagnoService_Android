package com.netease.ldnetdiagnoservicedemo_android;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.netease.LDNetDiagnoService.LDNetDiagnoListener;
import com.netease.LDNetDiagnoService.LDNetDiagnoService;
import com.sonuan.library.email.EmailHandler;

import javax.mail.MessagingException;

public class MainActivity extends Activity implements OnClickListener,
    LDNetDiagnoListener {
  private Button btn;
  private ProgressBar progress;
  private TextView text;
  private EditText edit;
  private String showInfo = "";
  private boolean isRunning = false;
  private LDNetDiagnoService _netDiagnoService;
  private String mTag;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main);
    btn = (Button) findViewById(R.id.btn);
    btn.setOnClickListener(this);
    progress = (ProgressBar) findViewById(R.id.progress);
    progress.setVisibility(View.INVISIBLE);
    text = (TextView) findViewById(R.id.text);
    edit = (EditText) findViewById(R.id.domainName);
    edit.clearFocus();
  }

  @Override
  public void onClick(View v) {
    if (v == btn) {
      mTag = System.currentTimeMillis()+"";
      text.setTag(mTag);
      if (!isRunning) {
        showInfo = "";
        String domainName = edit.getText().toString().trim();
        _netDiagnoService = new LDNetDiagnoService(getApplicationContext(),
            "NetworkDiagnosis", "网络诊断应用", "1.0.0", "huipang@corp.netease.com",
            "deviceID(option)", domainName, "", "",
            "", "", this);
        // 设置是否使用JNIC 完成traceroute
        _netDiagnoService.setIfUseJNICTrace(true);
//        _netDiagnoService.setIfUseJNICConn(true);
        _netDiagnoService.execute();
        progress.setVisibility(View.VISIBLE);
        text.setText("Traceroute with max 30 hops... " + "\n");
        btn.setText("停止诊断");
        btn.setEnabled(false);
        edit.setInputType(InputType.TYPE_NULL);
      } else {
        progress.setVisibility(View.GONE);
        btn.setText("开始诊断");
        _netDiagnoService.cancel(true);
        _netDiagnoService = null;
        btn.setEnabled(true);
        edit.setInputType(InputType.TYPE_CLASS_TEXT);
      }

      isRunning = !isRunning;
    }
  }

  private void sendEmail(String log) {
    EmailHandler emailHandler = new EmailHandler(new EmailHandler.OnProgressListener() {
      @Override
      public void onStart() {

      }

      @Override
      public void onSuccess() {
        Toast.makeText(MainActivity.this, "诊断信息上传成功", Toast.LENGTH_SHORT).show();
      }

      @Override
      public void onFailure() {
        Toast.makeText(MainActivity.this, "诊断信息上传失败", Toast.LENGTH_SHORT).show();
      }
    });
    try {
      emailHandler.setDebug(true);
      emailHandler.setProperties("smtp.qq.com", "465", true);
      emailHandler.setReceivers("wusongyuan@taqu.cn");
      emailHandler.setMessage(***REMOVED***,"【网络诊断】", log);
      emailHandler.sendEmail("smtp.qq.com", ***REMOVED***, ***REMOVED***);
    } catch (MessagingException e) {
      e.printStackTrace();
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (_netDiagnoService != null) {
      _netDiagnoService.stopNetDialogsis();
    }

  }

  @Override
  public void onBackPressed() {

    super.onBackPressed();

  }

  @Override
  public void OnNetDiagnoFinished(String log) {
    //text.setText(log);
    System.out.println("");
    progress.setVisibility(View.GONE);
    btn.setText("开始诊断");
    btn.setEnabled(true);
    edit.setInputType(InputType.TYPE_CLASS_TEXT);
    isRunning = false;

    sendEmail(log);
  }

  @Override
  public void OnNetDiagnoUpdated(String log) {
    if (mTag.equals(text.getTag())) {
      text.append(log);
      text.requestFocus();
    }else{
      text.setText(log);
    }
  }
}
