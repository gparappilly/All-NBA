package com.gmail.jorgegilcavazos.ballislife.data.service;

import com.gmail.jorgegilcavazos.ballislife.features.model.SubscriberCount;
import com.gmail.jorgegilcavazos.ballislife.util.RedditUtils;
import com.gmail.jorgegilcavazos.ballislife.util.exception.ReplyNotAvailableException;
import com.gmail.jorgegilcavazos.ballislife.util.exception.ReplyToCommentException;
import com.gmail.jorgegilcavazos.ballislife.util.exception.ReplyToThreadException;

import net.dean.jraw.RedditClient;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.http.SubmissionRequest;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthHelper;
import net.dean.jraw.managers.AccountManager;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.CommentSort;
import net.dean.jraw.models.Contribution;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.Subreddit;
import net.dean.jraw.models.VoteDirection;
import net.dean.jraw.paginators.SubredditPaginator;
import net.dean.jraw.paginators.UserContributionPaginator;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.CompletableOnSubscribe;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;

@Singleton
public class RedditServiceImpl implements RedditService {

    @Inject
    public RedditServiceImpl() {
    }

    @Override
    public Single<List<Contribution>> getUserContributions(
            final UserContributionPaginator paginator) {
        return Single.create(new SingleOnSubscribe<List<Contribution>>() {
            @Override
            public void subscribe(SingleEmitter<List<Contribution>> e) throws Exception {
                try {
                    e.onSuccess(new ArrayList<>(paginator.next()));
                } catch (Exception ex) {
                    if (!e.isDisposed()) {
                        e.onError(ex);
                    }
                }
            }
        });
    }

    @Override
    public Single<List<CommentNode>> getComments(
            final RedditClient redditClient,
            final String threadId,
            final String type) {
        return Single.create(new SingleOnSubscribe<List<CommentNode>>() {
            @Override
            public void subscribe(SingleEmitter<List<CommentNode>> e) throws Exception {
                SubmissionRequest.Builder builder = new SubmissionRequest.Builder(threadId);
                switch (type) {
                    case RedditUtils.LIVE_GT_TYPE:
                        builder.sort(CommentSort.NEW);
                        break;
                    case RedditUtils.POST_GT_TYPE:
                        builder.sort(CommentSort.TOP);
                        break;
                    default:
                        builder.sort(CommentSort.TOP);
                        break;
                }

                SubmissionRequest submissionRequest = builder.build();
                Submission submission = null;
                try {
                    submission = redditClient.getSubmission(submissionRequest);

                    Iterable<CommentNode> iterable = submission.getComments().walkTree();
                    List<CommentNode> commentNodes = new ArrayList<>();
                    for (CommentNode node : iterable) {
                        commentNodes.add(node);
                    }

                    if (!e.isDisposed()) {
                        e.onSuccess(commentNodes);
                    }
                } catch (Exception ex) {
                    if (!e.isDisposed()) {
                        e.onError(ex);
                    }
                }
            }
        });
    }

    @Override
    public Single<CommentNode> getComment(
            final RedditClient redditClient,
            final String threadId,
            final String commentId) {
        return Single.create(new SingleOnSubscribe<CommentNode>() {
            @Override
            public void subscribe(SingleEmitter<CommentNode> e) throws Exception {
                SubmissionRequest.Builder builder = new SubmissionRequest.Builder(threadId);
                builder.sort(CommentSort.NEW);

                SubmissionRequest submissionRequest = builder.build();
                Submission submission = null;
                try {
                    submission = redditClient.getSubmission(submissionRequest);

                    Iterable<CommentNode> iterable = submission.getComments().walkTree();
                    for (CommentNode node : iterable) {
                        if (node.getComment().getId().equals(commentId)) {
                            if (!e.isDisposed()) {
                                e.onSuccess(node);
                                return;
                            }
                        }
                    }

                    if (!e.isDisposed()) {
                        e.onError(new ReplyNotAvailableException());
                    }
                } catch (NetworkException ex) {
                    if (!e.isDisposed()) {
                        e.onError(ex);
                    }
                }
            }
        });
    }

    @Override
    public Single<String> replyToComment(
            final RedditClient redditClient,
            final Comment parent,
            final String text) {
        return Single.create(new SingleOnSubscribe<String>() {
            @Override
            public void subscribe(SingleEmitter<String> e) throws Exception {
                AccountManager accountManger = new AccountManager(redditClient);
                try {
                    String id = accountManger.reply(parent, text);
                    e.onSuccess(id);
                } catch (Exception ex) {
                    if (!e.isDisposed()) {
                        e.onError(new ReplyToCommentException());
                    }
                }
            }
        });
    }

    @Override
    public Completable voteComment(
            final RedditClient redditClient,
            final Comment comment,
            final VoteDirection direction) {
        return Completable.create(new CompletableOnSubscribe() {
            @Override
            public void subscribe(CompletableEmitter e) throws Exception {
                AccountManager accountManager = new AccountManager(redditClient);
                try {
                    accountManager.vote(comment, direction);
                    e.onComplete();
                } catch (Exception ex) {
                    if (!e.isDisposed()) {
                        e.onError(ex);
                    }
                }
            }
        });
    }

    @Override
    public Completable saveComment(final RedditClient redditClient, final Comment comment) {
        return Completable.create(new CompletableOnSubscribe() {
            @Override
            public void subscribe(CompletableEmitter e) throws Exception {
                AccountManager accountManager = new AccountManager(redditClient);
                try {
                    accountManager.save(comment);
                    e.onComplete();
                } catch (Exception ex) {
                    if (!e.isDisposed()) {
                        e.onError(ex);
                    }
                }
            }
        });
    }

    @Override
    public Completable unsaveComment(final RedditClient redditClient, final Comment comment) {
        return Completable.create(new CompletableOnSubscribe() {
            @Override
            public void subscribe(CompletableEmitter e) throws Exception {
                AccountManager accountManager = new AccountManager(redditClient);
                try {
                    accountManager.unsave(comment);
                    e.onComplete();
                } catch (Exception ex) {
                    if (!e.isDisposed()) {
                        e.onError(ex);
                    }
                }
            }
        });
    }

    @Override
    public Single<String> replyToThread(
            final RedditClient redditClient,
            final Submission submission,
            final String text) {
        return Single.create((e) -> {
            AccountManager accountManager = new AccountManager(redditClient);
            try {
                e.onSuccess(accountManager.reply(submission, text));
            } catch (Exception ex) {
                if (!e.isDisposed()) {
                    e.onError(new ReplyToThreadException());
                }
            }
        });
    }

    @Override
    public Single<Submission> getSubmission(
            final RedditClient redditClient,
            final String threadId,
            final CommentSort sort) {
        return Single.create(new SingleOnSubscribe<Submission>() {
            @Override
            public void subscribe(SingleEmitter<Submission> e) throws Exception {
                SubmissionRequest.Builder builder = new SubmissionRequest.Builder(threadId);
                if (sort != null) {
                    builder.sort(sort);
                }

                SubmissionRequest submissionRequest = builder.build();

                try {
                    e.onSuccess(redditClient.getSubmission(submissionRequest));
                } catch (Exception ex) {
                    if (!e.isDisposed()) {
                        e.onError(ex);
                    }
                }
            }
        });
    }

    @Override
    public Single<Listing<Submission>> getSubmissionListing(final SubredditPaginator paginator) {
        return Single.create(new SingleOnSubscribe<Listing<Submission>>() {
            @Override
            public void subscribe(SingleEmitter<Listing<Submission>> e) throws Exception {
                try {
                    e.onSuccess(paginator.next(false));
                } catch (Exception ex) {
                    if (!e.isDisposed()) {
                        e.onError(ex);
                    }
                }
            }
        });
    }

    @Override
    public Completable voteSubmission(
            final RedditClient redditClient,
            final Submission submission,
            final VoteDirection vote) {
        return Completable.create(new CompletableOnSubscribe() {
            @Override
            public void subscribe(CompletableEmitter e) throws Exception {
                AccountManager accountManager = new AccountManager(redditClient);
                try {
                    accountManager.vote(submission, vote);
                    e.onComplete();
                } catch (Exception ex) {
                    if (!e.isDisposed()) {
                        e.onError(ex);
                    }
                }
            }
        });
    }

    @Override
    public Completable saveSubmission(
            final RedditClient redditClient,
            final Submission submission,
            final boolean saved) {
        return Completable.create(new CompletableOnSubscribe() {
            @Override
            public void subscribe(CompletableEmitter e) throws Exception {
                AccountManager accountManager = new AccountManager(redditClient);
                try {
                    if (saved) {
                        accountManager.save(submission);
                    } else {
                        accountManager.unsave(submission);
                    }
                    e.onComplete();
                } catch (Exception ex) {
                    if (!e.isDisposed()) {
                        e.onError(ex);
                    }
                }
            }
        });
    }

    @Override
    public Single<SubscriberCount> getSubscriberCount(
            final RedditClient redditClient,
            final String subreddit) {
        return Single.create(new SingleOnSubscribe<SubscriberCount>() {
            @Override
            public void subscribe(SingleEmitter<SubscriberCount> e) throws Exception {
                try {
                    Subreddit rnba = redditClient.getSubreddit(subreddit);
                    Long subscribers = rnba.getSubscriberCount();
                    int activeUsers = rnba.getAccountsActive();

                    e.onSuccess(new SubscriberCount(subscribers, activeUsers));
                } catch (Exception ex) {
                    if (!e.isDisposed()) {
                        e.onError(ex);
                    }
                }
            }
        });
    }

    @Override
    public Completable userlessAuthentication(final RedditClient reddit,
                                              final Credentials credentials) {
        return Completable.create(new CompletableOnSubscribe() {
            @Override
            public void subscribe(CompletableEmitter e) throws Exception {
                try {
                    OAuthData oAuthData = reddit.getOAuthHelper().easyAuth(credentials);
                    reddit.authenticate(oAuthData);
                    e.onComplete();
                } catch (Exception ex) {
                    if (!e.isDisposed()) {
                        e.onError(ex);
                    }
                }
            }
        });
    }

    @Override
    public Completable userAuthentication(final RedditClient reddit, final Credentials credentials,
                                          final String url) {
        return Completable.create(new CompletableOnSubscribe() {
            @Override
            public void subscribe(CompletableEmitter e) throws Exception {
                OAuthHelper oAuthHelper = reddit.getOAuthHelper();

                try {
                    OAuthData oAuthData = oAuthHelper.onUserChallenge(url, credentials);
                    reddit.authenticate(oAuthData);
                    e.onComplete();
                } catch (Exception ex) {
                    if (!e.isDisposed()) {
                        e.onError(ex);
                    }
                }
            }
        });
    }

    @Override
    public Completable refreshToken(final RedditClient reddit, final Credentials credentials,
                                          final String refreshToken) {
        return Completable.create(new CompletableOnSubscribe() {
            @Override
            public void subscribe(CompletableEmitter e) throws Exception {
                OAuthHelper helper = reddit.getOAuthHelper();
                helper.setRefreshToken(refreshToken);

                try {
                    OAuthData oAuthData = helper.refreshToken(credentials);
                    reddit.authenticate(oAuthData);
                    e.onComplete();
                } catch (Exception ex) {
                    if (!e.isDisposed()) {
                        e.onError(ex);
                    }
                }
            }
        });
    }

    @Override
    public Completable deAuthenticate(final RedditClient reddit, final Credentials credentials) {
        return Completable.create(new CompletableOnSubscribe() {
            @Override
            public void subscribe(CompletableEmitter e) throws Exception {
                OAuthHelper helper = reddit.getOAuthHelper();
                try {
                    helper.revokeAccessToken(credentials);
                    reddit.deauthenticate();
                    e.onComplete();
                } catch (Exception ex) {
                    if (!e.isDisposed()) {
                        e.onError(ex);
                    }
                }
            }
        });
    }
}
