package com.jerboa.ui.components.post

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.jerboa.*
import com.jerboa.datatypes.SortType
import com.jerboa.db.AccountViewModel
import com.jerboa.ui.components.comment.CommentNode
import com.jerboa.ui.components.comment.edit.CommentEditViewModel
import com.jerboa.ui.components.comment.edit.commentEditClickWrapper
import com.jerboa.ui.components.comment.reply.CommentReplyViewModel
import com.jerboa.ui.components.comment.reply.commentReplyClickWrapper
import com.jerboa.ui.components.common.SimpleTopAppBar
import com.jerboa.ui.components.common.getCurrentAccount
import com.jerboa.ui.components.community.CommunityViewModel
import com.jerboa.ui.components.community.communityClickWrapper
import com.jerboa.ui.components.person.PersonProfileViewModel
import com.jerboa.ui.components.person.personClickWrapper
import com.jerboa.ui.components.post.edit.PostEditViewModel
import com.jerboa.ui.components.post.edit.postEditClickWrapper
import com.jerboa.ui.components.report.CreateReportViewModel
import com.jerboa.ui.components.report.commentReportClickWrapper
import com.jerboa.ui.components.report.postReportClickWrapper

@Composable
fun PostActivity(
    postViewModel: PostViewModel,
    communityViewModel: CommunityViewModel,
    personProfileViewModel: PersonProfileViewModel,
    accountViewModel: AccountViewModel,
    commentEditViewModel: CommentEditViewModel,
    commentReplyViewModel: CommentReplyViewModel,
    postEditViewModel: PostEditViewModel,
    createReportViewModel: CreateReportViewModel,
    navController: NavController,
) {

    Log.d("jerboa", "got to post activity")

    val ctx = LocalContext.current
    val listState = rememberLazyListState()

    val account = getCurrentAccount(accountViewModel = accountViewModel)
    val commentNodes = sortNodes(buildCommentsTree(postViewModel.comments), SortType.Hot)

    val swipeRefreshState = rememberSwipeRefreshState(
        isRefreshing = postViewModel.loading && postViewModel
            .postView.value !== null
    )

    Surface(color = MaterialTheme.colors.background) {
        Scaffold(
            topBar = {
                Column {
                    SimpleTopAppBar("Comments", navController = navController)
                    if (postViewModel.loading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            content = {
                SwipeRefresh(
                    state = swipeRefreshState,
                    onRefresh = {
                        postViewModel.postView.value?.also { postView ->
                            postViewModel.fetchPost(
                                id = postView.post.id,
                                account = account,
                                ctx = ctx,
                            )
                        }
                    },
                ) {
                    postViewModel.postView.value?.also { postView ->
                        LazyColumn(state = listState) {
                            item {
                                PostListing(
                                    postView = postView,
                                    fullBody = true,
                                    onUpvoteClick = {
                                        postViewModel.likePost(
                                            voteType = VoteType.Upvote,
                                            account = account,
                                            ctx = ctx,
                                        )
                                        // TODO will need to pass in postlistingsviewmodel
                                        // for the Home page to also be updated
                                    },
                                    onDownvoteClick = {
                                        postViewModel.likePost(
                                            voteType = VoteType.Downvote,
                                            account = account,
                                            ctx = ctx,
                                        )
                                    },
                                    onSaveClick = {
                                        postViewModel.savePost(
                                            account = account,
                                            ctx = ctx
                                        )
                                    },
                                    onReplyClick = { postView ->
                                        commentReplyClickWrapper(
                                            commentReplyViewModel = commentReplyViewModel,
                                            postId = postView.post.id,
                                            postView = postView,
                                            navController = navController,
                                        )
                                    },
                                    onPostLinkClick = { url ->
                                        openLink(url, ctx)
                                    },
                                    onCommunityClick = { community ->
                                        communityClickWrapper(
                                            communityViewModel,
                                            community.id,
                                            account,
                                            navController,
                                            ctx
                                        )
                                    },
                                    onPersonClick = { personId ->
                                        personClickWrapper(
                                            personProfileViewModel,
                                            personId,
                                            account,
                                            navController,
                                            ctx
                                        )
                                    },
                                    onEditPostClick = { postView ->
                                        postEditClickWrapper(
                                            postEditViewModel,
                                            postView,
                                            navController,
                                        )
                                    },
                                    onReportClick = { postView ->
                                        postReportClickWrapper(
                                            createReportViewModel,
                                            postView.post.id,
                                            navController,
                                        )
                                    },
                                    showReply = true,
                                    account = account,
                                    isModerator = isModerator(postView.creator, postViewModel.moderators)
                                )
                            }
                            // Don't use CommentNodes here, otherwise lazy scrolling wont work
                            // Can't really do scrolling well here either because of tree
                            itemsIndexed(commentNodes) { _, node ->
                                CommentNode(
                                    node = node,
                                    onUpvoteClick = { commentView ->
                                        account?.also { acct ->
                                            postViewModel.likeComment(
                                                commentView = commentView,
                                                voteType = VoteType.Upvote,
                                                account = acct,
                                                ctx = ctx,
                                            )
                                        }
                                    },
                                    onDownvoteClick = { commentView ->
                                        account?.also { acct ->
                                            postViewModel.likeComment(
                                                commentView = commentView,
                                                voteType = VoteType.Downvote,
                                                account = acct,
                                                ctx = ctx,
                                            )
                                        }
                                    },
                                    onReplyClick = { commentView ->
                                        commentReplyClickWrapper(
                                            commentReplyViewModel = commentReplyViewModel,
                                            parentCommentView = commentView,
                                            postId = commentView.post.id,
                                            navController = navController,
                                        )
                                    },
                                    onSaveClick = { commentView ->
                                        account?.also { acct ->
                                            postViewModel.saveComment(
                                                commentView = commentView,
                                                account = acct,
                                                ctx = ctx,
                                            )
                                        }
                                    },
                                    onPersonClick = { personId ->
                                        personClickWrapper(
                                            personProfileViewModel,
                                            personId,
                                            account,
                                            navController,
                                            ctx
                                        )
                                    },
                                    onEditCommentClick = { commentView ->
                                        commentEditClickWrapper(
                                            commentEditViewModel,
                                            commentView,
                                            navController,
                                        )
                                    },
                                    onReportClick = { commentView ->
                                        commentReportClickWrapper(
                                            createReportViewModel,
                                            commentView.comment.id,
                                            navController,
                                        )
                                    },
                                    account = account,
                                    moderators = postViewModel.moderators,
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}
