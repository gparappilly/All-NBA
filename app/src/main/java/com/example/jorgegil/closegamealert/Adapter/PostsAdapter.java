package com.example.jorgegil.closegamealert.Adapter;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.jorgegil.closegamealert.R;
import com.example.jorgegil.closegamealert.Utils.Utilities;
import com.squareup.picasso.Picasso;

import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;

/**
 * Created by TOSHIBA on 12/25/2015.
 */
public class PostsAdapter extends BaseAdapter {
    private Context context;
    private Listing<Submission> postsList;
    private String type;
    private static LayoutInflater inflater = null;

    public PostsAdapter(Context context, Listing<Submission> postsList, String type) {
        this.context = context;
        this.postsList = postsList;
        this.type = type;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return postsList.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView;
        switch (type) {
            case "small":
                rowView = inflater.inflate(R.layout.post_layout, parent, false);
                break;
            case "large":
                rowView = inflater.inflate(R.layout.post_layout_large, parent, false);
                break;
            default:
                rowView = inflater.inflate(R.layout.post_layout, parent, false);
        }

        TextView scoreView = (TextView) rowView.findViewById(R.id.scoreView);
        TextView authorView = (TextView) rowView.findViewById(R.id.authorView);
        TextView createdView = (TextView) rowView.findViewById(R.id.createdView);
        TextView titleView = (TextView) rowView.findViewById(R.id.titleView);
        TextView numOfCommentsView = (TextView) rowView.findViewById(R.id.numCommentsView);
        TextView linkView = (TextView) rowView.findViewById(R.id.linkView);

        ImageView thumbnail = (ImageView) rowView.findViewById(R.id.thumbnail);

        Submission post = postsList.get(position);

        titleView.setText(post.getTitle());
        if (post.isStickied()) {
            titleView.setTextColor(ContextCompat.getColor(context, R.color.stickiedColor));
        }
        scoreView.setText(String.valueOf(post.getScore()));
        authorView.setText(post.getAuthor());
        createdView.setText(Utilities.formatRedditDate(post.getCreated()));
        numOfCommentsView.setText(post.getCommentCount() + " Comments");

        String thumbnailUrl = post.getThumbnail();

        if (post.isSelfPost()) {
            linkView.setText("• self." + post.getSubredditName());
            thumbnail.setVisibility(View.GONE);
        } else {
            linkView.setText("• link (" + post.getDomain() + ")");
            if (thumbnailUrl != null) {
                Picasso.with(context).load(thumbnailUrl).into(thumbnail);
            } else {
                thumbnail.setVisibility(View.GONE);
            }
        }

        return rowView;
    }

}