package org.fruct.oss.getssupplement.overlays;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.fruct.oss.gets.Category;
import org.fruct.oss.gets.Disability;
import org.fruct.oss.gets.PointsService;
import org.fruct.oss.getssupplement.R;

import java.util.ArrayList;
import java.util.List;

/**
 * отображение точек препятствий
 */

public class OverlayFiltersFragment extends ListFragment {

    private List<Disability> disabilities = new ArrayList<>();

    private List<Category> categories = new ArrayList<>();

    private DisabilityAdapter adapter;

    private PointsService pointsService;
    private ServiceConnection pointConnection = new PointConnection();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.overlay_filter, null);

        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Bundle GeTS service
        Intent intent = new Intent(getActivity(), PointsService.class);
        getActivity().bindService(intent, pointConnection, Context.BIND_AUTO_CREATE);

        adapter = new DisabilityAdapter(getContext(),
                R.layout.list_disability_item, disabilities, categories);
        setListAdapter(adapter);
    }

    public void setDisabilities(List<Disability> disabilitiesList) {
        this.disabilities.clear();
        this.disabilities.addAll(disabilitiesList);
        adapter.notifyDataSetInvalidated();
        Log.d(getClass().getSimpleName(), "Got " + this.disabilities.size() + " items");

    }

    public void setCategories(List<Category> categoriesList) {
        this.categories.clear();
        this.categories.addAll(categoriesList);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        Log.d(getClass().getSimpleName(), "Hidden = " + hidden);

        if (hidden == true && pointsService != null) {
            for(Disability d : disabilities) {
                pointsService.setDisabilityState(d, d.isActive());
            }

            for(Category c : categories) {
                pointsService.setCategoryState(c, c.isActive());
            }
            pointsService.notifyDataUpdated(false);
        }
        super.onHiddenChanged(hidden);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Log.d(getClass().getSimpleName(), "Select item at position: " + position);
        View checkBox = v.findViewById(R.id.disability_checked);
        if (checkBox != null) {
            Log.d(getClass().getSimpleName(), "Item check is " + ((CheckBox)checkBox).isChecked());
        }
        super.onListItemClick(l, v, position, id);
    }

    // когда установлен коннект с pointsService
    private void onPointsServiceReady(PointsService service) {
        this.pointsService = service;
        //getActivity().supportInvalidateOptionsMenu();
    }

    @Override
    public void onDestroy() {
        if (pointsService != null) {
            pointsService = null;
        }

        getActivity().unbindService(pointConnection);super.onDestroy();
    }

    private class DisabilityAdapter extends ArrayAdapter {

        private List<Disability> disabilities;

        private List<Category> categories;

        DisabilityAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<Disability> objects, @NonNull List<Category> categoriesList) {
            super(context, resource, objects);
            disabilities = objects;
            this.categories = categoriesList;
        }

        @Override
        public int getCount() {
            return disabilities.size() + categories.size() + 2; // 2 элемента на хедеры
        }

        @Nullable
        @Override
        public Object getItem(int position) {
            if (position == 0 || position == disabilities.size() + 1)
                return null;
            if (position <=disabilities.size())
                return disabilities.get(position);

            return categories.get(position - disabilities.size() - 2);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (position == 0) {
                // шапка инвалидностей
                View header_view = LayoutInflater.from(getContext()).inflate(R.layout.item_header, null);
                ((TextView) header_view.findViewById(R.id.header_name)).setText("Disabilities");

                return header_view;
            }
            if (position > 0 && position <= disabilities.size()) {
                Disability d = disabilities.get(position - 1);
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_disability_item, null);
                //((ImageView)convertView.findViewById(android.R.id.icon)).setImageBitmap(d.get);
                ((TextView) convertView.findViewById(R.id.disability_name)).setText(d.getName());
                ((CheckBox) convertView.findViewById(R.id.disability_checked)).setChecked(d.isActive());
                ((CheckBox) convertView.findViewById(R.id.disability_checked)).setOnCheckedChangeListener(checkListener);
                convertView.findViewById(R.id.disability_checked).setTag(position);

                return convertView;
            }
            if (position == disabilities.size() + 1) {
                // шапка инвалидностей
                View header_view = LayoutInflater.from(getContext()).inflate(R.layout.item_header, null);
                ((TextView) header_view.findViewById(R.id.header_name)).setText("Categories");

                return header_view;
            }

            // рисуем категорию
            Category c = categories.get(position - disabilities.size() - 2);
            View view = LayoutInflater.from(getContext()).inflate(R.layout.list_disability_item, null);
            ((TextView) view.findViewById(R.id.disability_name)).setText(c.getName());
            ((CheckBox) view.findViewById(R.id.disability_checked)).setChecked(c.isActive());
            ((CheckBox) view.findViewById(R.id.disability_checked)).setOnCheckedChangeListener(checkListener);
            view.findViewById(R.id.disability_checked).setTag(position);
            ((ImageView) view.findViewById(R.id.disability_icon)).setImageBitmap(c.getIcon());
            view.setTag(position);
            return view;
        }

        CompoundButton.OnCheckedChangeListener checkListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                Log.d(getClass().getSimpleName(), "Tag: " + buttonView.getTag());
                int position = ((Integer)buttonView.getTag());
                if (position <= disabilities.size()) {
                    disabilities.get(position - 1).setActive(isChecked);
                } else {
                    categories.get(position - disabilities.size() - 2).setActive(isChecked);
                }
            }
        };
    }

    // соединение с PointsService
    private class PointConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            PointsService service = ((PointsService.Binder) binder).getService();
            onPointsServiceReady(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            pointsService = null;
        }
    }


}
