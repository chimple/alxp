package org.chimple.messenger.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.chimple.messenger.ImApp;
import org.chimple.messenger.MainActivity;
import org.chimple.messenger.crypto.IOtrChatSession;
import org.chimple.messenger.crypto.OtrChatManager;
import org.chimple.messenger.model.Contact;
import org.chimple.messenger.model.ImErrorInfo;
import org.chimple.messenger.plugin.xmpp.XmppAddress;
import org.chimple.messenger.provider.Imps;
import org.chimple.messenger.service.IChatSession;
import org.chimple.messenger.service.IChatSessionManager;
import org.chimple.messenger.service.IContactListManager;
import org.chimple.messenger.service.IImConnection;
import org.chimple.messenger.tasks.ChatSessionInitTask;
import org.chimple.messenger.tasks.LoopbackChatSessionInitTask;
import org.chimple.messenger.ui.legacy.DatabaseUtils;
import org.chimple.messenger.ui.onboarding.OnboardingManager;
import org.chimple.messenger.ui.qr.QrDisplayActivity;
import org.chimple.messenger.ui.qr.QrShareAsyncTask;
import org.chimple.messenger.ui.widgets.LetterAvatar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import cn.pedant.SweetAlert.SweetAlertDialog;
import im.zom.messenger.R;


public class ContactDisplayActivity extends BaseActivity {

    private int mContactId = -1;
    private String mNickname = null;
    private String mUsername = null;
    private long mProviderId = -1;
    private long mAccountId = -1;
    private IImConnection mConn;
    private String mRemoteFingerprint;

    private String getPathToIconFile(final String folder) {
        ImApp app = (ImApp) getApplicationContext();
        String path = app.getFilesDir().getAbsolutePath() + File.separator + folder + File.separator + folder + "_big.png";
        return path;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.awesome_activity_contact);


        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        applyStyleForToolbar();

        mContactId = (int)getIntent().getLongExtra("contactId",-1);

        mNickname = getIntent().getStringExtra("nickname");
        mUsername = getIntent().getStringExtra("address");
        mProviderId = getIntent().getLongExtra("provider", -1);
        mAccountId = getIntent().getLongExtra("account", -1);

        mConn = ((ImApp)getApplication()).getConnection(mProviderId,mAccountId);

        if (TextUtils.isEmpty(mNickname)) {
            mNickname = mUsername;
            mNickname = mNickname.split("@")[0].split("\\.")[0];
        }

        setTitle("");
       
        TextView tv = (TextView)findViewById(R.id.tvNickname);
        tv = (TextView)findViewById(R.id.tvNickname);
        tv.setText(mNickname);

        tv = (TextView)findViewById(R.id.tvUsername);
        tv.setText(mUsername);

        if (!TextUtils.isEmpty(mUsername)) {
            try {
                Drawable avatar = DatabaseUtils.getAvatarFromAddress(getContentResolver(), mUsername, ImApp.DEFAULT_AVATAR_WIDTH, ImApp.DEFAULT_AVATAR_HEIGHT, false);
                if (avatar != null) {
                    ImageView iv = (ImageView) findViewById(R.id.imageAvatar);
                    iv.setImageDrawable(avatar);
                    iv.setVisibility(View.VISIBLE);
                }
                else
                {
                    // int color = getAvatarBorder(presence);
                    ImageView iv = (ImageView) findViewById(R.id.imageAvatar);
                    int padding = 24;
                    String iconPath = getPathToIconFile(mNickname);
                    Drawable drawableFromDownload = Drawable.createFromPath(iconPath);
                    if(drawableFromDownload==null)  //   file is not found in download folder
                    {
                        try {
                            InputStream inputStream = getApplicationContext().getAssets().open(mNickname+"/"+mNickname+"_big.png");
                            Drawable drawableFromAssets = Drawable.createFromStream(inputStream, null);
                            if(drawableFromAssets==null)
                            {
                                LetterAvatar lavatar = new LetterAvatar(getApplicationContext(), mNickname, padding);
                                iv.setImageDrawable(lavatar);
                            }
                            else
                            {
                                iv.setImageDrawable(drawableFromAssets);
                            }
                        }
                        catch(Exception e)
                        {
                            LetterAvatar lavatar = new LetterAvatar(getApplicationContext(), mNickname, padding);
                            iv.setImageDrawable(lavatar);
                        }
                    }
                    else
                        iv.setImageDrawable(drawableFromDownload);
                }
            } catch (Exception e) {
            }
        }

        ImageView btnQrShare = (ImageView) findViewById(R.id.qrshare);
        ImageView iv = (ImageView)findViewById(R.id.qrcode);
        tv = (TextView)findViewById(R.id.tvFingerprint);

        Button btnVerify = (Button)findViewById(R.id.btnVerify);
        btnVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyRemoteFingerprint();
                findViewById(R.id.btnVerify).setVisibility(View.GONE);
            }
        });

        try {

            mRemoteFingerprint = OtrChatManager.getInstance().getRemoteKeyFingerprint(mUsername);

            if (mRemoteFingerprint != null) {

                if (!TextUtils.isEmpty(mRemoteFingerprint)) {
                    tv.setText(prettyPrintFingerprint(mRemoteFingerprint));

                    iv.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {

                            String inviteString;
                            try {
                                inviteString = OnboardingManager.generateInviteLink(ContactDisplayActivity.this, mUsername, mRemoteFingerprint, mNickname);

                                Intent intent = new Intent(ContactDisplayActivity.this, QrDisplayActivity.class);
                                intent.putExtra(Intent.EXTRA_TEXT, inviteString);
                                intent.setType("text/plain");
                                startActivity(intent);

                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }

                        }

                    });

                    btnQrShare.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            try {
                                String inviteLink = OnboardingManager.generateInviteLink(ContactDisplayActivity.this, mUsername, mRemoteFingerprint, mNickname);
                                new QrShareAsyncTask(ContactDisplayActivity.this).execute(inviteLink, mNickname);
                            } catch (IOException ioe) {
                                Log.e(ImApp.LOG_TAG, "couldn't generate QR code", ioe);
                            }
                        }
                    });

                    if (OtrChatManager.getInstance().isRemoteKeyVerified(mUsername))
                        btnVerify.setVisibility(View.GONE);


                } else {
                    iv.setVisibility(View.GONE);
                    tv.setVisibility(View.GONE);
                    btnQrShare.setVisibility(View.GONE);
                    btnVerify.setVisibility(View.GONE);

                }
            }
            else {
                iv.setVisibility(View.GONE);
                tv.setVisibility(View.GONE);
                btnQrShare.setVisibility(View.GONE);
                btnVerify.setVisibility(View.GONE);
            }
        }
        catch (Exception e)
        {
            Log.e(ImApp.LOG_TAG,"error displaying contact",e);
        }

        View btn = findViewById(R.id.btnStartChat);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toolbar.setTitle(mNickname);
                if(mProviderId != 2) {
                    startChat();
                } else {
                    startLoopbackChat();
                }


            }
        });

//        if (mContactId != -1)
  //          showGallery (mContactId);

    }

    private void showGallery (int contactId)
    {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        GalleryListFragment fragment = new GalleryListFragment();
        Bundle args = new Bundle();
        args.putInt("contactId", contactId);
        fragment.setArguments(args);
        fragmentTransaction.add(R.id.fragment_container, fragment, "MyActivity");
        fragmentTransaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

     //  getMenuInflater().inflate(R.menu.menu_contact_detail, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_verify_or_view:
                verifyRemoteFingerprint();
                return true;
            case R.id.menu_verify_question:
                initSmpUI();
                return true;
            case R.id.menu_remove_contact:
                deleteContact();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String prettyPrintFingerprint(String fingerprint) {
        StringBuffer spacedFingerprint = new StringBuffer();

        for (int i = 0; i + 8 <= fingerprint.length(); i += 8) {
            spacedFingerprint.append(fingerprint.subSequence(i, i + 8));
            spacedFingerprint.append(' ');
        }

        return spacedFingerprint.toString();
    }

    void deleteContact ()
    {

        new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                .setTitleText(getString(R.string.menu_remove_contact))
                .setContentText(getString(R.string.confirm_delete_contact, mNickname))
                .setConfirmText(getString(R.string.ok))
                .setCancelText(getString(R.string.cancel))
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        doDeleteContact();
                        sDialog.dismiss();
                        finish();
                        startActivity(new Intent(ContactDisplayActivity.this, MainActivity.class));
                    }
                })
                .show();
    }

    void doDeleteContact ()
    {
        try {

            IImConnection mConn;
            mConn = ((ImApp)getApplication()).getConnection(mProviderId, mAccountId);

            IContactListManager manager = mConn.getContactListManager();

            int res = manager.removeContact(mUsername);
            if (res != ImErrorInfo.NO_ERROR) {
                //mHandler.showAlert(R.string.error,
                //      ErrorResUtils.getErrorRes(getResources(), res, address));
            }

        }
        catch (RemoteException re)
        {

        }
    }

    private void startChat ()
    {
        boolean startCrypto = true;

        new ChatSessionInitTask(((ImApp)getApplication()),mProviderId, mAccountId, Imps.Contacts.TYPE_NORMAL, startCrypto)
        {
            @Override
            protected void onPostExecute(Long chatId) {

                if (chatId != -1) {
                    Intent intent = new Intent(ContactDisplayActivity.this, ConversationDetailActivity.class);
                    intent.putExtra("id", chatId);
                    startActivity(intent);
                }

                super.onPostExecute(chatId);
            }
        }.executeOnExecutor(ImApp.sThreadPoolExecutor,mUsername);

        finish();

    }

    private void startLoopbackChat ()
    {
        new LoopbackChatSessionInitTask(((ImApp)getApplication()),mProviderId, mAccountId, Imps.Contacts.TYPE_NORMAL)
        {
            @Override
            protected void onPostExecute(Long chatId) {

                if (chatId != -1) {
                    Intent intent = new Intent(ContactDisplayActivity.this, ConversationDetailActivity.class);
                    intent.putExtra("nickname",mNickname);
                    intent.putExtra("id", chatId);
                    startActivity(intent);
                }

                super.onPostExecute(chatId);
            }
        }.executeOnExecutor(ImApp.sThreadPoolExecutor,mUsername);

        finish();

    }

    private void initSmp(String question, String answer) {
        try {


                IChatSessionManager manager = mConn.getChatSessionManager();
                IChatSession session = manager.getChatSession(mUsername);
                IOtrChatSession iOtrSession = session.getDefaultOtrChatSession();
                iOtrSession.initSmpVerification(question, answer);


        } catch (RemoteException e) {
            Log.e(ImApp.LOG_TAG, "error init SMP", e);

        }
    }

    private void verifyRemoteFingerprint() {


        try {
            if (mConn != null) {
                IContactListManager listManager = mConn.getContactListManager();

                if (listManager != null)
                    listManager.approveSubscription(new Contact(new XmppAddress(mUsername), mNickname));

                IChatSessionManager manager = mConn.getChatSessionManager();

                if (manager != null) {
                    IChatSession session = manager.getChatSession(mUsername);

                    if (session != null) {
                        IOtrChatSession otrChatSession = session.getDefaultOtrChatSession();

                        if (otrChatSession != null) {
                            otrChatSession.verifyKey(otrChatSession.getRemoteUserId());
                            Snackbar.make(findViewById(R.id.main_content), getString(R.string.action_verified), Snackbar.LENGTH_LONG).show();
                        }
                    }
                }

            }

        } catch (RemoteException e) {
            Log.e(ImApp.LOG_TAG, "error init otr", e);

        }

    }

    private void initSmpUI() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View viewSmp = inflater.inflate(R.layout.smp_question_dialog, null, false);

        if (viewSmp != null)
        {
            new AlertDialog.Builder(this).setTitle(this.getString(R.string.otr_qa_title)).setView(viewSmp)
                    .setPositiveButton(this.getString(R.string.otr_qa_send), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            EditText eiQuestion = (EditText) viewSmp.findViewById(R.id.editSmpQuestion);
                            EditText eiAnswer = (EditText) viewSmp.findViewById(R.id.editSmpAnswer);
                            String question = eiQuestion.getText().toString();
                            String answer = eiAnswer.getText().toString();
                            initSmp(question, answer);
                        }
                    }).setNegativeButton(this.getString(R.string.otr_qa_cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Do nothing.
                }
            }).show();
        }
    }



}
