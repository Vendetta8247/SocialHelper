package ua.com.vendetta8247.testproject;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class VKFragment extends Fragment {
    TextView tv;
    JSONObject jsonResponse;
    String responseWall = "";
    RecyclerView recyclerViewVK;
    VKAdapter adapter;
    Thread t;
    Bitmap mIcon11 = null;
    Status status;
    enum Status {NOT_LOGGED_IN, LOGGING_IN, LOGGED_IN};                                             //перечисление для идентификации статуса приложения. При первоначальном запуске приложения
                                                                                                    //статус NOT_LOGGED_IN, после открытия окна авторизации он становится LOGGING_IN
                                                                                                    //это делается для того, чтобы параллельный поток не начинал загрузку данных, пока пользователь не войдет

    List<VKCard> vkCards;
    private OnFragmentInteractionListener mListener;

    public VKFragment() {
    }

    public static VKFragment newInstance(String param1, String param2) {
        VKFragment fragment = new VKFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vk, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerViewVK = (RecyclerView) getActivity().findViewById(R.id.recyclerVK);
        adapter = new VKAdapter();                                                                  //добавление адаптера, viewholder'a и LayoutManager'a для RecyclerView
        recyclerViewVK.setAdapter(adapter);
        recyclerViewVK.setLayoutManager(new LinearLayoutManager(getActivity()));

        vkCards = new ArrayList<>();


        status = Status.LOGGED_IN;



        MyApplication app = new MyApplication();

        if(VKSdk.isLoggedIn())
            status = Status.LOGGED_IN;

        if(!VKSdk.isLoggedIn()) {
            status = Status.NOT_LOGGED_IN;
                    VKSdk.login(getActivity(), "wall, friends");                                    //Вход в аккаунт пользователя с возможностью получать записи со стены и записи друзей
                                                                                                    //В эту категорию также попадают записи сообществ
            status = Status.LOGGING_IN;

        }


        t = new Thread(new Runnable() {
            @Override
            public void run() {                                                           //Данный поток необходим, чтобы циклично проверять, вошел ли пользователь в аккаунт
                while(status == Status.LOGGING_IN) {                                                // Такая проверка необходима, т.к. логин происходит асинхронно и элементы интерфейса
                    try {                                                                           // начинают загрузку параллельно с логином. Таким образом, если пользователь не залогинен,
                        Thread.currentThread().sleep(100);                                          // при первом запуске не будут отображаться новости. Будет ошибка "неавторизированный пользователь"
                        if (VKSdk.isLoggedIn())
                            break;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
                loadNews();

            }
        });
        t.start();
    }


    class AsyncVKPostsLoader extends AsyncTask<Void, Void, Void>
    {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                JSONArray array = jsonResponse.getJSONObject("response").getJSONArray("items");                        //получаем массив всех записей

                for(int i = 0; i<array.length(); i++) {

                    if(array.getJSONObject(i).get("type").equals("post")) {                                            //фильтр записей, которые не являются постами
                        if (array.getJSONObject(i).has("post_id")) {
                            if(i>0 && !array.getJSONObject(i-1).toString().equals(array.getJSONObject(i).toString()))  //иногда API возвращает копии записей. Эта проверка их убирает
                            {

                                responseWall = array.getJSONObject(i)
                                        .get("text") + "\n";
                                Log.d("VK", array.getJSONObject(i).toString());
                            }
                            else
                            {
                                continue;

                            }
                        } else
                        {
                            responseWall = "";
                            continue;
                        }


                        if (array.getJSONObject(i).has("attachments") && array.getJSONObject(i).getJSONArray("attachments").getJSONObject(0).has("photo")) {
                            JSONObject photos = array.getJSONObject(i).getJSONArray("attachments").getJSONObject(0).getJSONObject("photo");                     //Если в записи есть фотография, получаем ссылку на неё


                            try {
                                InputStream in = new java.net.URL(photos.get("photo_604").toString()).openStream();
                                mIcon11 = BitmapFactory.decodeStream(in);                                                                                       //загрузка фотографии по ссылке
                            } catch (Exception e) {

                                e.printStackTrace();
                            }


                        if (!responseWall.equals("") || mIcon11 != null)
                            vkCards.add(new VKCard(responseWall, mIcon11));                                                                                     //заполняем список полученными постами
                        }
                    }
                }} catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {                                                  // все UI операции должны проходить на UI потоке. Этот метод принадлежит к нему
            super.onPostExecute(aVoid);



            for(VKCard c : vkCards)
            adapter.addItem(c);                                                                     //После загрузки всех постов добавляем полученные карточки в UI
        }
    }


    public void loadNews()
    {
        final VKRequest request = new VKRequest("newsfeed.get", VKParameters.from(VKApiConst.FIELDS, "items"));     //запрос к API ВК. newsfeed.get позволяет получить список новостей
        request.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onError(VKError error) {
                System.out.println("error" + error.toString());

                super.onError(error);
            }

            @Override
            public void onComplete(VKResponse response) {
                super.onComplete(response);

                System.out.println("success");
                jsonResponse = response.json;                                                       //получаем json из response

                new AsyncVKPostsLoader().execute();                                                 //в случае успешной загрузки запускаем задание по парсингу JSONа и переводу полученных данных в UI

            }

            @Override
            public void attemptFailed(VKRequest request, int attemptNumber, int totalAttempts) {
                super.attemptFailed(request, attemptNumber, totalAttempts);
            }
        });

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
        if (!VKSdk.onActivityResult(requestCode, resultCode, data, new VKCallback<VKAccessToken>() {
            @Override
            public void onResult(VKAccessToken res) {
                System.out.println("Authorization successful");                                     //Для авторизации необходимо использовать onActivityResult, с помощью него можно понять, прошла операция успешно или нет
            }
            @Override
            public void onError(VKError error) {
                System.out.println("Authorization error");
            }
        }))
        {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    class VKAdapter extends RecyclerView.Adapter<VKViewHolder>                                      //Кастомный адаптер, внутри которого определяем поведение и внешний вид нашего списка
    {                                                                                               //Для работы с RecyclerView такая модель необходима
        private List<VKCard> cardList;

        public VKAdapter()
        {
            cardList = new ArrayList<>();
        }


        @Override
        public VKViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View itemView;
            VKViewHolder holder;


            itemView = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.card_vk, parent, false);                                       //Получаем внешний вид карточки для поста из layout'а
            holder = new VKViewHolder(itemView);

            return holder;
        }

        @Override
        public void onBindViewHolder(VKViewHolder holder, int position) {                           //по привязке ViewHolder'а мы задаем значения из списка созданных при асинхронном задании заранее карточек
            VKCard card = cardList.get(position);
            holder.VKPostText.setText(card.trimmedText);
            holder.profilePic.setImageBitmap(card.image);
        }

        @Override
        public int getItemCount() {
            return cardList.size();
        }

        public void addItem(VKCard card)
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

    class VKViewHolder extends RecyclerView.ViewHolder
    {

        protected TextView VKPostText;
        protected ImageView profilePic;


        public VKViewHolder(View itemView) {
            super(itemView);
            VKPostText = (TextView) itemView.findViewById(R.id.vkCardText);
            profilePic=(ImageView)itemView.findViewById(R.id.vkCardImage);



            itemView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {                                             // При нажатии на карточку откроется полный текст сообщения
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

                            intent.putExtra("fullText", VKPostText.getText());
                            if(VKPostText.getText().length()>1)
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


    class VKCard
    {
        String trimmedText;
        Bitmap image;
        public VKCard(String trimmedText, Bitmap image)
        {
            this.trimmedText = trimmedText;
            this.image = image;
        }
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}
