package com.example.osvaldoairon.app4so;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.osvaldoairon.app4so.ActivitysSecond.ActivityInf;
import com.example.osvaldoairon.app4so.ActivitysSecond.ActivityPesquisa;
import com.example.osvaldoairon.app4so.EventBus.MessageEvent;
import com.example.osvaldoairon.app4so.Fragments.MapGoogleActivity;
import com.example.osvaldoairon.app4so.Modelo.Coordenadas;
import com.example.osvaldoairon.app4so.Sqlite.HelperBuscas;
import com.example.osvaldoairon.app4so.service.LocalizacaoIntentService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
{


    public static final String LOCATION = "location";
    public static final String ADRESS = "adress";
    public static final String TYPE = "type";

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggler;
    private MapGoogleActivity mapFragmento;
    private FragmentManager fragmentManager;
    private static ArrayList<Coordenadas> list_main;
    private EditText edtbusca;
    private ImageButton imgBusca;
    private LatLng location;
    private ArrayList<LatLng> list_buscas;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private GoogleMap map;
    private LatLng coordenadasBuscadas;

    private HelperBuscas helper;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        list_main = new ArrayList<Coordenadas>();

        list_buscas = new ArrayList<LatLng>();

        iniciarFirebaseDatabase();
        helper = new HelperBuscas(MainActivity.this);


        edtbusca=(EditText)findViewById(R.id.edtLocal);
        imgBusca=(ImageButton)findViewById(R.id.imgBuscar);


        EventBus.getDefault().register(this);


        drawerLayout = (DrawerLayout)findViewById(R.id.drawerLayout);
        actionBarDrawerToggler = new ActionBarDrawerToggle(this,drawerLayout,R.string.Abr,R.string.Fec);
        actionBarDrawerToggler.setDrawerIndicatorEnabled(true);



        drawerLayout.addDrawerListener(actionBarDrawerToggler);
        actionBarDrawerToggler.syncState();

        NavigationView view = (NavigationView)findViewById(R.id.nav_Mapa);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true); //carrega o botao home no actionbar

        view.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();

                if(id == R.id.config){
                    Toast.makeText(MainActivity.this, "não implementado", Toast.LENGTH_SHORT).show();
                }else if(id == R.id.exibir_inf){
                    /*
                    exibir informações;
                     */
                    Intent at = new Intent(MainActivity.this, ActivityInf.class);
                    at.putExtra("arrayCidades",list_main);
                    startActivity(at);
                }else if(id == R.id.rotas){
                    /*
                    botao rotas;
                     */
                }else{
                    /*
                    Pesquisas SAlvas;
                     */
                    Intent at = new Intent(MainActivity.this,ActivityPesquisa.class);
                    startActivity(at);

                }


                return true;
            }
        });

        imgBusca.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(edtbusca.getText().toString().isEmpty() || edtbusca.toString().length() > 20){
                    String endereco = edtbusca.getText().toString();
                    int type = 1;
                    callIntentService(type,endereco);
                }else{
                    Toast.makeText(MainActivity.this,"Preencha o campo corretamente",Toast.LENGTH_SHORT).show();
                }

            }
        });

        /*
        Gerenciador de fragmentos fragmentManager

        FragmentTransaction = inicia uma transacao de fragmento podendo iniciar ou parar uma "transaction"

        é necessario dizer o layout que o fragmento vai ser carregado , e uma nova instanciacao do objeto fragmento
        pelo meotodo fragmentTransaction.add(layout,fragmento.obj, "string");

        iniciar o fragmento chamando o
        fragmentTransaction.commit() ou comit()AllowingStateLoss();
         */
        helper.recoverDataSQL();
        MapGoogleActivity act = new MapGoogleActivity(MainActivity.this,helper.getReturnList());
        act.actived();

        fragmentManager = getSupportFragmentManager();

        FragmentTransaction fg = fragmentManager.beginTransaction();

        fg.add(R.id.relativeLayoutx,new MapGoogleActivity(), "MapGOOGLE");
        fg.commitAllowingStateLoss();
    }

    public void callIntentService(int tipo , String endereco){
        /*
        Conecta o service passando 3 parametros
        endereço
        tipo
        e a minha localizaçãoatual;
         */
        Intent it = new Intent(MainActivity.this, LocalizacaoIntentService.class);
        it.putExtra(TYPE,tipo);
        it.putExtra(ADRESS,endereco);
        it.putExtra(LOCATION,location);
        startService(it);
    }

    public LatLng recebeLocalizacao(LatLng localizacao){

        if(localizacao!=null){
            location =localizacao;
            return location;
        }
        return null;

    }
    @Subscribe
    public void onEvent(final MessageEvent event){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                Log.d("Resultado endereco", ""+event.getData());

                if(event.getData()!=null){
                    String[] saida = event.getData().split("\n");
                    double latitude = Double.parseDouble(saida[0]);
                    double longitude = Double.parseDouble(saida[1]);
                    if(latitude != 0 && longitude != 0){
                        //Toast.makeText(MainActivity.this,""+latitude,Toast.LENGTH_SHORT).show();
                        Coordenadas coordenadasb = new Coordenadas();
                        coordenadasb.setLatitude(latitude);
                        coordenadasb.setLongitude(longitude);
                        coordenadasb.setNomePontoTuristico(edtbusca.getText().toString());
                        helper.inserir(coordenadasb);

                        updateSearch();

                    }
                }

            }
        });
    }

    public void iniciarFirebaseDatabase(){
        FirebaseApp.initializeApp(MainActivity.this);
        firebaseDatabase = firebaseDatabase.getInstance();
        //fireBaseDatabase.setPersistenceEnabled(true);
        databaseReference = firebaseDatabase.getReference();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return  actionBarDrawerToggler.onOptionsItemSelected(item)||super.onOptionsItemSelected(item);
    }

    public ArrayList<Coordenadas> recebeArraymain(ArrayList<Coordenadas> list){

        if(list!=null){
//            Toast.makeText(MainActivity.this, "DOIDERA", Toast.LENGTH_SHORT).show();
            list_main = list;
            return list_main;
        }
        return null;
    }


    public MainActivity(){}

    public void updateSearch(){
        helper.recoverDataSQL();
        MapGoogleActivity act = new MapGoogleActivity(MainActivity.this,helper.getReturnList());
        act.actived();
        FragmentTransaction fg = fragmentManager.beginTransaction();
        fg.add(R.id.relativeLayoutx,new MapGoogleActivity(), "MapGOOGLE");
        fg.commitAllowingStateLoss();

    }


}
