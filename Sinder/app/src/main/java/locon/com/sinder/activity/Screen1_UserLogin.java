package locon.com.sinder.activity;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

import locon.com.sinder.R;
import locon.com.sinder.utility.UtilityConstants;


public class Screen1_UserLogin extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener{

    private LoginButton facebookHidden;
    private SignInButton googleHidden;
    private Button fbActual;
    private Button googleActual;
    private CallbackManager callbackManager;
    private boolean facebookUser = false;
    private GoogleApiClient apiClient;
    private static final int RC_SIGN_IN = 0;
    private boolean google_sign_in_initiated = false;
    private static final int STATE_DEFAULT = 0;
    private static final int STATE_SIGN_IN = 1;
    private static final int STATE_IN_PROGRESS = 2;
    private int signInProgress;
    private Person mPerson;
    private PendingIntent mSignInIntent;
    private int mSignInError;
    private String email;
    private String username;
    private String user_type;
    private JSONArray friend_list_array;
    private ProgressDialog pDialog;
    private TextView appNameTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initiateServices();
        checkExistingUser();
        setContentView(R.layout.activity_screen1_user_login);
        getObjects();
        initialize();
        registerCallbacks();
        setListeners();
    }

    private void setListeners(){
        fbActual.setOnClickListener(this);
        googleActual.setOnClickListener(this);
        googleHidden.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.googleButton:
                signInProgress = STATE_SIGN_IN;
                apiClient.connect();
                google_sign_in_initiated = true;
                googleHidden.callOnClick();
                break;
            case R.id.facebookButton:
                facebookHidden.callOnClick();
                break;
            case R.id.googleHidden:
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        apiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(apiClient.isConnected()){
            apiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        this.mPerson = Plus.PeopleApi.getCurrentPerson(apiClient);
        email = Plus.AccountApi.getAccountName(apiClient);
        signInProgress = STATE_DEFAULT;
        username = mPerson.getDisplayName();
        user_type = UtilityConstants.GOOGLE_USER;
    }

    @Override
    public void onConnectionSuspended(int i) {
        apiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if(connectionResult.getErrorCode() == ConnectionResult.API_UNAVAILABLE){
            Log.w("ConnectionFailed","API unavailable");
            Toast.makeText(this, "API unavailable", Toast.LENGTH_SHORT).show();
        }else if(signInProgress != STATE_IN_PROGRESS){
            mSignInIntent = connectionResult.getResolution();
            mSignInError = connectionResult.getErrorCode();
            if(signInProgress == STATE_SIGN_IN){
                resolveSignInError();
            }
        }
    }

    private void resolveSignInError(){
        if(mSignInIntent!=null){
            try{
                signInProgress = STATE_IN_PROGRESS;
                startIntentSenderForResult(mSignInIntent.getIntentSender(),RC_SIGN_IN,null,0,0,0);
            }catch(IntentSender.SendIntentException e){
                Toast.makeText(this, "Sign in intent could not be sent!", Toast.LENGTH_SHORT).show();
                signInProgress = STATE_SIGN_IN;
                apiClient.connect();
            }
        }
    }

    private void initiateServices(){
        apiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API, Plus.PlusOptions.builder().build())
                .addScope(new Scope(Scopes.PROFILE))
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .build();
    }

    private void checkExistingUser(){
        FacebookSdk.sdkInitialize(getApplicationContext());
        if(AccessToken.getCurrentAccessToken() == null){
            facebookUser = false;
        }else{
            Intent i = new Intent(this, Screen2_PlayAndRecommend.class);
            startActivity(i);
            Screen1_UserLogin.this.finish();
        }
    }

    private void initialize(){
        callbackManager = CallbackManager.Factory.create();
        Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/lovelo_line_bold.otf");
        appNameTV.setTypeface(tf);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(google_sign_in_initiated){
            if (requestCode == RC_SIGN_IN) {
                if(resultCode == RESULT_OK){
                    signInProgress = STATE_SIGN_IN;
                }else{
                    signInProgress = STATE_DEFAULT;
                }
                if(!apiClient.isConnecting())
                    apiClient.connect();
            }
        }else {
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void registerCallbacks(){
        facebookHidden.setReadPermissions(Arrays.asList("basic_info", "public_profile", "user_friends", "email"));
        facebookHidden.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                if (loginResult.getAccessToken()
                        != null) {
                    GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                        @Override
                        public void onCompleted(JSONObject jsonObject, GraphResponse graphResponse) {
                            try {
                                email = jsonObject.getString("email");
                                username = jsonObject.getString("name");
                            } catch (JSONException e) {
                                Toast.makeText(Screen1_UserLogin.this, "Unable to fetch email!", Toast.LENGTH_SHORT).show();
                            }
                            new GraphRequest(
                                    AccessToken.getCurrentAccessToken(),
                                    "/me/friends",
                                    null,
                                    HttpMethod.GET,
                                    new GraphRequest.Callback() {
                                        public void onCompleted(GraphResponse response) {
                                            Toast.makeText(Screen1_UserLogin.this, response.toString(), Toast.LENGTH_LONG).show();
                                            Log.d("DataAsJSON", response.getJSONObject().toString());
                                            try{
                                            JSONArray obj = response.getJSONObject().getJSONArray("data");
                                                for(int i = 0 ; i < obj.length(); i++){
                                                    friend_list_array.put(obj.getJSONObject(i));
                                                }
                                            }catch(Exception e){
                                                Log.e("GetFacebookFriends",e.getMessage());
                                            }
                                        }
                                    }
                            ).executeAsync();
                        }
                    });
                    Bundle parameters = new Bundle();
                    parameters.putString("fields", "email,name");
                    request.setParameters(parameters);
                    request.executeAsync();
                    user_type = UtilityConstants.FACEBOOK_USER;
                }
            }

            @Override
            public void onCancel() {
                Toast.makeText(Screen1_UserLogin.this, "Facebook login cancelled!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException e) {
                Toast.makeText(Screen1_UserLogin.this, "Error occured while logging into facebook!", Toast.LENGTH_SHORT).show();
                ;
            }
        });
    }

    private void getObjects(){
        facebookHidden = (LoginButton) findViewById(R.id.facebookHidden);
        googleHidden = (SignInButton) findViewById(R.id.googleHidden);
        fbActual = (Button) findViewById(R.id.facebookButton);
        googleActual = (Button) findViewById(R.id.googleButton);
        appNameTV = (TextView) findViewById(R.id.app_name);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_screen1_user_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppEventsLogger.activateApp(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        AppEventsLogger.deactivateApp(this);
    }

    private void saveUserDetails(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(UtilityConstants.USERNAME, username);
        editor.putString(UtilityConstants.EMAIL, email);
        editor.putString(UtilityConstants.USER_TYPE, user_type);
        editor.apply();

    }

    class SendUserDetails extends AsyncTask{
        @Override
        protected Object doInBackground(Object[] params) {
            JSONObject obj = new JSONObject();
            try {

            }catch(Exception e){

            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(Screen1_UserLogin.this);
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            Toast.makeText(Screen1_UserLogin.this, "Object created!", Toast.LENGTH_SHORT).show();
        }
    }
}