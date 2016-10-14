package ua.com.vendetta8247.testproject;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class FacebookFragment extends Fragment {

    CallbackManager callbackManager;
    String accessToken;
    String pageId;
    private OnFragmentInteractionListener mListener;
    FacebookAdapter adapter;
    List<FacebookCard> facebookCards;

    RecyclerView recyclerViewFacebook;

    public FacebookFragment() {
    }

    public static FacebookFragment newInstance(String param1, String param2) {
        FacebookFragment fragment = new FacebookFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_facebook, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new FacebookAdapter();
        recyclerViewFacebook = (RecyclerView) getActivity().findViewById(R.id.recyclerFacebook); //список, в который будут добавляться карточки
        recyclerViewFacebook.setAdapter(adapter);                                                //привязка созданного адаптера
        recyclerViewFacebook.setLayoutManager(new LinearLayoutManager(getActivity()));           //для работы RecyclerView необходимо, чтоб у него был LayoutManager. Использую стандартный
        facebookCards = new ArrayList<>();                                                       //список карточек
        if(AccessToken.getCurrentAccessToken()!=null) {
            accessToken = AccessToken.getCurrentAccessToken().getToken();                        //получение токена доступа, необходимого для запросов
            pageId = AccessToken.getCurrentAccessToken().getUserId();                            //ИД пользователя, использующего приложение
            new AsyncFacebookLoader().execute();                                                 // запуск задания, которое получает список постов, в фоновом потоке
        }

        callbackManager = CallbackManager.Factory.create();

        final LoginButton loginButton = (LoginButton) view.findViewById(R.id.login_button);      //Стандартная кнопка для логина в Facebook
        if(pageId!=null)
            loginButton.setVisibility(View.GONE);                                                //Как только произошел логин, кнопка исчезает, вместо неё появляется список
            loginButton.setReadPermissions("user_posts");                                        //получение разрешения на чтение постов в Facebook
        loginButton.setFragment(this);
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d("Facebook", "Success");
                accessToken = loginResult.getAccessToken().getToken();
                Log.d("Access token", accessToken);
                pageId = AccessToken.getCurrentAccessToken().getUserId();
                loginButton.setVisibility(View.GONE);
                new AsyncFacebookLoader().execute();


            }

            @Override
            public void onCancel() {
            }

            @Override
            public void onError(FacebookException exception) {
                exception.printStackTrace();
            }
        });
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode,resultCode, data);
    }

    class AsyncFacebookLoader extends AsyncTask<Void, Void, Void>
    {
        @Override
        protected Void doInBackground(Void... params) {

            try {
                URL url = new URL("https://graph.facebook.com/" + pageId + "/feed?access_token=" +accessToken);         //Создается URL для запроса, которое содержит accessToken полученный при авторизации и
                                                                                                                        //ИД пользователя, также полученный при авторизации
                HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));         //По созданному URL открывается соединение и считывается полученный результат
                StringBuffer json = new StringBuffer(1024);
                String tmp="";
                while((tmp=reader.readLine())!=null)
                    json.append(tmp).append("\n");
                reader.close();

                JSONArray array = new JSONObject(json.toString()).getJSONArray("data");                                 // Отсюда и далее используются стандартные классы, связанные с обработкой JSON
                for(int i =0; i< array.length(); i++)
                {


                    if(array.getJSONObject(i).has("story"))                                                             // в случае с моим профилем, я обнаружил, что есть два типа постов - story и message
                        facebookCards.add(new FacebookCard(array.getJSONObject(i).get("story").toString(),null));       // Поэтому я сделал одинаковую обработку для обоих, получая текст истории и сообщения.
                    if(array.getJSONObject(i).has("message"))
                        facebookCards.add(new FacebookCard(array.getJSONObject(i).get("message").toString(),null));


                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            for(FacebookCard c : facebookCards)
            adapter.addItem(c);
        }
    }





    class FacebookAdapter extends RecyclerView.Adapter<FacebookViewHolder>
    {
        private List<FacebookCard> cardList;

        public FacebookAdapter()
        {
            cardList = new ArrayList<>();
        }


        @Override
        public FacebookViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View itemView;
            FacebookViewHolder holder;


            itemView = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.card_facebook, parent, false);
            holder = new FacebookViewHolder(itemView);

            return holder;
        }

        @Override
        public void onBindViewHolder(FacebookViewHolder holder, int position) {
            FacebookCard card = cardList.get(position);
            holder.FacebookPostText.setText(card.trimmedText);
            holder.profilePic.setImageBitmap(card.image);
        }

        @Override
        public int getItemCount() {
            return cardList.size();
        }

        public void addItem(FacebookCard card)
        {
            cardList.add(card);
            notifyItemInserted(cardList.size()-1);
        }

        public void clearList()
        {
            cardList.clear();
            notifyDataSetChanged();
        }
    }

    class FacebookViewHolder extends RecyclerView.ViewHolder
    {

        protected TextView FacebookPostText;
        protected ImageView profilePic;


        public FacebookViewHolder(View itemView) {
            super(itemView);
            FacebookPostText = (TextView) itemView.findViewById(R.id.facebookCardText);
            profilePic=(ImageView)itemView.findViewById(R.id.facebookCardImage);



            itemView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN: {
                            CardView view = (CardView) v;
                            view.setCardBackgroundColor(0xFFF0F0F0);
                            v.invalidate();
                            break;
                        }
                        case MotionEvent.ACTION_UP: {
                            Intent intent = new
                                    Intent(v.getContext(), VKPostReader.class);

                            intent.putExtra("fullText", FacebookPostText.getText());
                            if(FacebookPostText.getText().length()>1)
                                v.getContext().startActivity(intent);
                        }
                        case MotionEvent.ACTION_CANCEL: {
                            CardView view = (CardView) v;
                            view.setCardBackgroundColor(0xFFEEEEEE);
                            view.invalidate();
                            break;
                        }
                    }
                    return true;
                }
            });


        }
    }


    class FacebookCard
    {
        String trimmedText;
        Bitmap image;
        public FacebookCard(String trimmedText, Bitmap image)
        {
            this.trimmedText = trimmedText;
            this.image = image;
        }
    }





    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}
