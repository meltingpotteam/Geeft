package samurai.geeft.android.geeft.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.baasbox.android.BaasHandler;
import com.baasbox.android.BaasResult;
import com.baasbox.android.BaasUser;

import java.util.Random;

import samurai.geeft.android.geeft.R;
import samurai.geeft.android.geeft.database.BaaSMail;
import samurai.geeft.android.geeft.interfaces.TaskCallbackBooleanToken;
import samurai.geeft.android.geeft.models.User;
import samurai.geeft.android.geeft.utilities.TagsValue;

/**
 * Created by joseph on 25/03/16.
 */
public class UsernameMailActivity extends AppCompatActivity implements TaskCallbackBooleanToken {

    private final String TAG = getClass().getSimpleName();
    private EditText mNickname,mEmail;
    private String mNewUsername,mNewEmail;
    private Button mButtonDone;
    private User mUser;
    private Random mRandom;
    private int mCode;

    //-------------------Macros
    private final int RESULT_OK = 1;
    private final int RESULT_FAILED = 0;
    private final int RESULT_SESSION_EXPIRED = -1;
    private ProgressDialog mProgressDialog;
    //-------------------

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.username_mail);

        mUser =new User(BaasUser.current().getName());
        Log.i("USERNAMEMAIL", "Inside UsernameMailActivity after inflating the layout.");
        mNickname=(EditText) findViewById(R.id.username_edittext);
        mEmail=(EditText) findViewById(R.id.email_edittext);
        mButtonDone=(Button) findViewById(R.id.username_request_button);
        mButtonDone.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i("USERNAMEMAIL", "Nickname" + mNickname.getText().toString() + "!");
                Log.i("USERNAMEMAIL", "Email" + mEmail.getText().toString() + "!");
                addNicknameEmail();
            }
        });
    }

    private void addNicknameEmail() {
        final BaasUser user;
        mNewUsername = mNickname.getText().toString();
        mNewEmail = mEmail.getText().toString().toLowerCase();
        if ((mNewUsername.isEmpty()) || (mNewUsername == null)) {
            Toast.makeText(UsernameMailActivity.this, R.string.no_valid_username_toast, Toast.LENGTH_SHORT).show();
        } else {
            String email = mEmail.getText().toString().toLowerCase();
            if (isValidEmail(email)) {
//              Set mail and username
                Log.i("USERNAMEMAIL", "Valid mail.");
                emailVerification(email);

            } else {
                Toast.makeText(UsernameMailActivity.this, R.string.no_valid_mail_toast, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void emailVerification(String email) {
        final String myName = BaasUser.current().getName().toString();
        final BaasUser user = BaasUser.current();
        if(mRandom ==null){
            mRandom = new Random();
            int min = 1000;
            int max = 9999;
            final int code = mRandom.nextInt(max - min + 1) + min;
            mCode = code;
            Log.i("USERNAMEMAIL", "Verification code: " + code);
        }
        sendMail(email);
        final android.support.v7.app.AlertDialog.Builder builder =
                new android.support.v7.app.AlertDialog.Builder(UsernameMailActivity.this,
                        R.style.AppCompatAlertDialogStyle);
        final EditText input = new EditText(this.getApplicationContext());
        input.setGravity(Gravity.CENTER_HORIZONTAL);
        input.setHint("Inserisci il codice di conferma");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHintTextColor(getResources().getColor(R.color.colorHintAccent));
        input.setTextColor(getResources().getColor(R.color.colorPrimaryText));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        builder.setView(input);
        builder.setMessage("Controlla il codice nella tua mail");
        //builder.setTitle("Inserire pasword");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!checkCode(mCode, input.getText().toString())) {
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                } else {
                    Log.i("USERNAMEMAIL", "Dentro onClick. ");
                    final ProgressDialog progressDialog = new ProgressDialog(UsernameMailActivity.this);
                    progressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    progressDialog.show();
                    progressDialog.setMessage("Salvataggio in corso...");
                    user.getScope(BaasUser.Scope.REGISTERED).put("username", mNewUsername);
                    user.getScope(BaasUser.Scope.REGISTERED).put("email", mNewEmail);
                    user.save(new BaasHandler<BaasUser>() {
                        @Override
                        public void handle(BaasResult<BaasUser> baasResult) {
                            if (progressDialog != null) {
                                progressDialog.dismiss();
                            }
                            if (baasResult.isSuccess()) {
                                mRandom = null;
                                mUser.setUsername(mNewUsername);
                                mUser.setEmail(mNewEmail);
                                Log.d(TAG, BaasUser.current()
                                        .getScope(BaasUser.Scope.REGISTERED)
                                        .put("username", mNewUsername).toString());
                                startMainActivity();
                            } else if (baasResult.isFailed()) {
                                showDescriptionFailDailog();
                            }

                        }
                    });
                }
            }
        });
        builder.setNegativeButton("Invia di nuovo", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                emailVerification(mNewEmail);
            }
        });
        builder.show();
    }

    public void done(boolean result,int resultToken){
        if(mProgressDialog!=null){
            mProgressDialog.dismiss();
        }
        if(result) {
            Toast.makeText(this, "Successo", Toast.LENGTH_SHORT);
        }
        else{
            if(resultToken == RESULT_SESSION_EXPIRED){
                Log.e(TAG,"Invalid Session token");
                startLoginActivity();
            }
            else{
                Log.e(TAG,"Error occured");
                new AlertDialog.Builder(UsernameMailActivity.this)
                        .setTitle("Errore")
                        .setMessage("Operazione non possibile. Riprovare più tardi.").show();
            }
        }
    }

    private void startLoginActivity(){
        Intent intent = new Intent(getApplicationContext(),LoginActivity.class);
        startActivity(intent);
    }

    public final static boolean isValidEmail(CharSequence target) {
        if (target == null)
            return false;

        return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    //Starts the MainActivity
    private void startMainActivity() {
        final Intent mMainIntent = new Intent(UsernameMailActivity.this,
                MainActivity.class);
        startActivity(mMainIntent);
        finish();
    }

    private void sendMail(String newMail){
        new BaaSMail(TagsValue.DEFAULT_EMAIL,newMail,mCode).execute();
    }
    private boolean checkCode(int code, String userCode){
        int userInput=0;
        if(userCode!=null && !userCode.isEmpty()){
            userInput = Integer.parseInt(userCode);
        }
        if (userCode.isEmpty()||code!=userInput) {
            final android.support.v7.app.AlertDialog.Builder builder =
                    new android.support.v7.app.AlertDialog.Builder(UsernameMailActivity.this,
                            R.style.AppCompatAlertDialogStyle);
            builder.setTitle("Errore");
            builder.setMessage("Codice non valido");
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    addNicknameEmail();
                }
            });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
            return false;
        }
        return true;
    }

    private void showDescriptionFailDailog() {
        new AlertDialog.Builder(getApplicationContext())
                .setTitle("Ooops...")
                .setMessage("Operazione non riuscita")
                .setPositiveButton("Riprova", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        addNicknameEmail();
                    }
                })
                .setNegativeButton("Cancella", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mProgressDialog!=null){
            mProgressDialog.dismiss();
        }
    }

}