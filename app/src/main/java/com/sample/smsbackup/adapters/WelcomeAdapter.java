package com.sample.smsbackup.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.sample.smsbackup.R;

public class WelcomeAdapter extends PagerAdapter {

    private Context context;
    private LayoutInflater layoutInflater;

    //Slides content
    public int[] slideImage = {
            R.drawable.logo_backup,
            R.drawable.logo_restore,
            R.drawable.logo_delete,
            R.drawable.logo_security};
    public String[] slideTitle = {"Backup", "Restore", "Delete", "Permissions"};
    public String[] slideDescription = {
            "This action will compare your local message with this stored on cloud and backup differences",
            "This action will compare your local message with this stored on cloud and restore differences",
            "This action will delete all your message",
            "To allow access to your message you have to give me permission. Messages are stored on your private Drive so sing in wit Google. Restore and delete requires temporary default app"};

    //Cnt
    public WelcomeAdapter(Context context){
        this.context = context;
    }

    @Override
    public int getCount() {
        return slideTitle.length;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == (RelativeLayout) object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {

        layoutInflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.layout_slide,container,false );

        ImageView imgLogo = view.findViewById(R.id.imgSlide);
        TextView txtHeader = view.findViewById(R.id.textSlideHeader);
        TextView txtDescription = view.findViewById(R.id.textSlideDescription);

        imgLogo.setImageResource(slideImage[position]);
        txtHeader.setText(slideTitle[position]);
        txtDescription.setText(slideDescription[position]);

        container.addView(view);

        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((RelativeLayout) object);
    }
}
