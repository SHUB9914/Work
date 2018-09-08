<?php

class Users
{
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Users
     * @apiName minimal user's details
     * @api {get} /user/[id] minimal user's details
     * @apiDescription Service to view the minimal details of an user.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam {integer}  id  User identifier.
     *
     * @apiUse MiniCardProfile
     * 
     * @apiUse USR001
     * @apiUse USR101
     */
    public function getMinimalDetails() { return; }


    /**
     * @apiVersion 1.0.0
     * @apiGroup Users
     * @apiName user's profile
     * @api {get} /user/[id] user's profile
     * @apiDescription Service to view an user's full profile.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam {integer}  id  User identifier.
     *
     * @apiUse CardProfile
     * 
     * @apiUse USR001
     * @apiUse USR101
     */
    public function getProfile() { return; }


    /**
     * @apiVersion 1.0.0
     * @apiGroup Users
     * @apiName wall
     * @api {get} /user/[id]/wall[/pos] wall
     * @apiDescription Service to view a user's wall.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam {string}   id      User's identifier.
     * @apiParam {string}   [pos]   Pagination position's identifier.
     *
     * @apiSuccess {string}     previous                    Pagination position identifier of the previous page of wall's spoks.
     * @apiSuccess {string}     next                        Pagination position identifier of the next page of wall's spoks.
     * @apiSuccess {object[]}   spoks                      Wall's spoks list, from the most recent to the least (10 per page).
     * @apiSuccess {string}     spok.id                    Spok's identifier.
     * @apiSuccess {string}     spok.type                  <a href="#lexicon-spoks-types">Spok's type</a>.
     * @apiSuccess {timestamp}  spok.launched              Original spok's launching timestamp.
     * @apiSuccess {string}     spok.text                  Original spok's text.
     * @apiSuccess {timestamp}  spok.respoked              [instance] Current re-spoked instance's timestamp.
     * @apiSuccess {string}     spok.curtext               [instance] Current re-spoked instance's text.
     * @apiSuccess {object}     spok.from                  [instance] Previous spoker details.
     * @apiSuccess {integer}    spok.from.id               [instance] Previous spoker's identifier.
     * @apiSuccess {string}     spok.from.name             [instance] Previous spoker's name.
     * @apiSuccess {string}     spok.from.picture          [instance] Previous spoker profile's picture.
     * @apiSuccess {string}     spokr.from.gender          [instance] Previous spoker's <a href="#lexicon-users-genders">gender</a>.
     * @apiSuccess {object}     spok.counters              Original spok's counters (since its very first launch).
     * @apiSuccess {integer}    spok.counter.nb_spoked     Number of re-spoke.
     * @apiSuccess {integer}    spok.counter.nb_scoped     Number of users related to this spok ("pending", "respoked" and "unspoked" instances).
     * @apiSuccess {integer}    spok.counter.nb_comments   Number of comments.
     * @apiSuccess {decimal}    spok.counter.distance      Spok's total travelling distance (in meters).
     * @apiSuccess {mixed}      content                         Spok's content, strongly related to the spok's type.
     * @apiSuccess {string}     content.picture_preview         [only if type is "picture"] Picture's preview URL.
     * @apiSuccess {string}     content.picture_full            [only if type is "picture"] Full picture's URL.
     * @apiSuccess {string}     content.animated_gif            [only if type is "animatedgif"] Animated gif's URL.
     * @apiSuccess {string}     content.video_preview           [only if type is "video"] Video's preview URL.
     * @apiSuccess {string}     content.video                   [only if type is "video"] Video's URL.
     * @apiSuccess {string}     content.sound_preview           [only if type is "sound"] Sound's preview URL (SoundCloud-like?).
     * @apiSuccess {string}     content.sound                   [only if type is "sound"] Sound's URL.
     * @apiSuccess {string}     content.url                     [only if type is "url"] URL's address.
     * @apiSuccess {string}     content.url_preview             [only if type is "url"] URL's preview.
     * @apiSuccess {string}     content.url_type                [only if type is "url"] URL's <a href="#lexicon-url-content-types">type</a>.
     * @apiSuccess {string}     content.url_title               [only if type is "url"] URL's title.
     * @apiSuccess {string}     content.url_text                [only if type is "url"] URL's description.
     * @apiSuccess {string}     content.rawtext                 [only if type is "rawtext"] Raw text.
     * @apiSuccess {string}     content.htmltext                [only if type is "htmltext"] HTML text.
     * @apiSuccess {string}     content.poll_title              [only if type is "poll"] Poll's title.
     * @apiSuccess {string}     content.poll_description        [only if type is "poll"] Poll's description.
     * @apiSuccess {object[]}   content.poll_questions          [only if type is "poll"] Poll's questions (ordered by ascending rank).
     * @apiSuccess {integer}    content.poll_quest.id           [only if type is "poll"] Poll question's identifier.
     * @apiSuccess {string}     content.poll_quest.question     [only if type is "poll"] Poll question's text.
     * @apiSuccess {object[]}   content.poll_quest.answers      [only if type is "poll"] Poll question's answers (ordered by ascending rank).
     * @apiSuccess {integer}    content.poll_quest.answ.id      [only if type is "poll"] Poll answer's identifier.
     * @apiSuccess {string}     content.poll_quest.answ.text    [only if type is "poll"] Poll answer's text.
     * @apiSuccess {string}     content.riddle_question         [only if type is "riddle"] Riddle's question.
     * @apiSuccess {string}     content.riddle_answer           [only if type is "riddle"] Riddle's answer.
     * 
     * 
     * @apiUse USR102
     */
    public function getWall() { return; }


    /**
     * @apiVersion 1.0.0
     * @apiGroup Users
     * @apiName follow / unfollow
     * @api {websocket} /profile/[id] follow / unfollow
     * @apiDescription Switch-service to follow/unfollow an user.
     * <br />If it is an unfollow action, two events are sent: <a href="#api-Users-follower__removed_">"follower (removed)"</a> and <a href="#api-Users-following__removed_">"following (removed)"</a>.
     * <br />Else, if it is a follow action:
     * - two events are sent: <a href="#api-Users-follower__added_">"follower (add)"</a> and <a href="#api-Users-following__added_">"following (add)"</a>
     * - add also a new "follower" <a href="#api-My_Account-notifications">notification</a> for the followed user
     * - if there is a mutual following (both user are following each other), then they are considered as friends. <a href="#api-Users-friend">"friend"</a> events are sent to both users and "friend" <a href="#api-My_Account-notifications">notifications</a> are added too to both users
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam {integer}  id  User's identifier.
     *
     * @apiUse USR001
     * @apiUse FLW101
     */
    public function follow() { return; }


    /**
     * @apiVersion 1.0.0
     * @apiGroup Users
     * @apiName followers
     * @api {get} /profile/[id]/followers[/pos] followers
     * @apiDescription Service to get the list of the followers of an user.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam {integer}  id      User's identifier.
     * @apiParam {string}   [pos]   Pagination position identifier.
     *
     * @apiSuccess {string}     previous            Pagination position identifier of the previous page of followers.
     * @apiSuccess {string}     next                Pagination position identifier of the next page of followers.
     * @apiSuccess {object}     followers           10 most recent followers.
     * @apiSuccess {integer}    follower.id         Follower's identifier.
     * @apiSuccess {string}     follower.nickname   Follower's nickname.
     * @apiSuccess {string}     follower.gender     Follower's <a href="#lexicon-users-genders">gender</a>.
     * @apiSuccess {string}     follower.picture    Follower's profile picture URL.
     * 
     * @apiUse USR001
     * @apiUse FLW001
     * @apiUse FLW102
     */
    public function getFollowers() { return; }


    /**
     * @apiVersion 1.0.0
     * @apiGroup Users
     * @apiName followings
     * @api {get} /profile/[id]/followings[/pos] followings
     * @apiDescription Service to get the list of the followings of an user.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam {integer}  id      User's identifier.
     * @apiParam {string}   [pos]   Pagination position identifier.
     *
     * @apiSuccess {string}     previous            Pagination position identifier of the previous page of followings.
     * @apiSuccess {string}     next                Pagination position identifier of the next page of followings.
     * @apiSuccess {object}     followings          10 most recent followings.
     * @apiSuccess {integer}    following.id        Following's identifier.
     * @apiSuccess {string}     following.nickname  Following's nickname.
     * @apiSuccess {string}     following.gender    Following's <a href="#lexicon-users-genders">gender</a>.
     * @apiSuccess {string}     following.picture   Following's profile picture URL.
     * 
     * @apiUse USR001
     * @apiUse FLW002
     * @apiUse FLW103
     */
    public function getFollowings() { return; }

    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Users
     * @apiName group (create)
     * @api {websocket} /groups group (create)
     * @apiDescription Service to create a group of users and/or phone's contacts.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam    {string}    title   Group title.
     *
     * @apiSuccess  {string}    id      Group identifier.
     * 
     * @apiUse RGX014
     * @apiUse RGX015
     * @apiUse GRP101
     */
    public function createGroup() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Users
     * @apiName group (update)
     * @api {websocket} /group/[id] group (update)
     * @apiDescription Service to change the name of a group.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam {string}   id      Group identifier.
     * @apiParam {string}   [name]  Group name.
     *
     * @apiUse GRP001
     * @apiUse GRP102
     */
    public function updateGroup() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Users
     * @apiName group (remove)
     * @api {websocket} /group/[id] group (remove)
     * @apiDescription Service to remove a group.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam {string}   id  Group identifier.
     *
     * @apiUse GRP001
     * @apiUse GRP103
     */
    public function removeGroup() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Users
     * @apiName groups
     * @api {get} /groups groups
     * @apiDescription Service to get the list of the groups.
     * 
     * @apiUse AuthRequired
     * 
     * @apiSuccess {object[]}   groups              List of groups.
     * @apiSuccess {integer}    group.id            Group identifier.
     * @apiSuccess {string}     group.title         Group title.
     * @apiSuccess {string[]}   group.nicknames     Nicknames / names of the 10 last added in the group.
     * @apiSuccess {integer}    group.nb_users      Number of total users within the group.
     * @apiSuccess {integer}    group.followers     Number of followers within the group.
     * @apiSuccess {integer}    group.contacts      Number of contact's phone within the group.
     * 
     * @apiUse GRP104
     */
    public function getGroups() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Users
     * @apiName group (add user)
     * @api {websocket} /group/[id] group (add user)
     * @apiDescription Service to add followers or a phone's contacts to a group.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam {integer}      id              Group identifier.
     * @apiParam {integer[]}    userids         Users identifier to be added to the group.
     * @apiParam {object[]}     contacts        Contact's phone to be added to the group.
     * @apiParam {string}       contact.name    Contact's name (as retrieved from the contacts phone book, which could actually be forenam + name).
     * @apiParam {string}       contact.phone   Contact's <a href="#lexicon-phone-number-format">phone number</a>.
     *
     * @apiUse GRP105
     */
    public function addUserToGroup() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Users
     * @apiName group (remove user)
     * @api {websocket} /group/[id] group (remove user)
     * @apiDescription Service to remove a follower or a phone's contact from a group.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam {integer}      id      Group identifier.
     * @apiParam {integer[]}    userids Users identifier to be removed.
     * @apiParam {string[]}     phones  Contact's <a href="#lexicon-phone-number-format">phone number</a> to be removed.
     *
     * @apiUse GRP106
     */
    public function removeUserFromGroup() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Users
     * @apiName following (added)
     * @api {websocket} / following (added)
     * @apiDescription Event emitted when <a href="#api-Users-follow___unfollow">an user "A" follows another user "B"</a>.
     * <br />Event received by the following user "A" and all the users currently visiting "A".
     * <br />User in the event is "A", the following one.
     * 
     * @apiUse AuthRequired
     * 
     * @apiUse EventUserStructure
     */
    public function newFollowing() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Users
     * @apiName following (removed)
     * @api {websocket} / following (removed)
     * @apiDescription Event emitted when <a href="#api-Users-follow___unfollow">an user "A" stop following another user "B"</a>.
     * <br />Event received by the user "A" and all the users currently visiting "A".
     * <br />User in the event is "A", the unfollowing one.
     * 
     * @apiUse AuthRequired
     * 
     * @apiUse EventUserStructure
     */
    public function removeFollowing() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Users
     * @apiName follower (added)
     * @api {websocket} / follower (added)
     * @apiDescription Event emitted when <a href="#api-Users-follow___unfollow">an user "A" follows another user "B"</a>.
     * <br />Event received by the followed user "B" and all the users currently visiting "B".
     * <br />User in the event is "B", the followed one.
     * 
     * @apiUse AuthRequired
     * 
     * @apiUse EventUserStructure
     */
    public function newFollower() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Users
     * @apiName follower (removed)
     * @api {websocket} / follower (removed)
     * @apiDescription Event emitted when <a href="#api-Users-follow___unfollow">an user "A" stop following another user "B"</a>.
     * <br />Event received by the unfollowed user "B" and all the users currently visiting "B".
     * <br />User in the event is "B", the unfollowed one.
     * 
     * @apiUse AuthRequired
     * 
     * @apiUse EventUserStructure
     */
    public function removeFollower() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Users
     * @apiName friend
     * @api {websocket} / friend
     * @apiDescription Event emitted when two users are mutually <a href="#api-Users-follow___unfollow">following</a> each other.
     * <br />Event received by both related users.
     * 
     * @apiUse AuthRequired
     * 
     * @apiExample {js} Emitted / received example
     * {
     *  friends: [see event user structure tab],
     * }
     * @apiUse EventUserStructure
     * 
     * @apiSuccess {object[]}  friends Array containing both friends (see user data structure below).
     */
    public function friend() { return; }
}