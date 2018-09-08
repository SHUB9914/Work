<?php

class Spok
{    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Spok
     * @apiName view one (short)
     * @api {get} /stack/[id]  view one (short)
     * @apiDescription Service to get quick view of one spok.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam {integer}  id      Spok identifier.
     *
     * @apiSuccess {string}     id                              Spoke's identifier.
     * @apiSuccess {string}     type                            <a href="#lexicon-spoks-types">Spok's type</a>.
     * @apiSuccess {timestamp}  ttl                             Spok's <a href="#lexicon-spok-TTL">TTL</a>.
     * @apiSuccess {timestamp}  launched                        Original spok's launching timestamp.
     * @apiSuccess {string}     text                            Original spok's text.
     * @apiSuccess {timestamp}  respoked                        [instance] Current re-spoked instance's timestamp.
     * @apiSuccess {string}     curtext                         [instance] Current re-spoked instance's text.
     * @apiSuccess {object}     spoker                          [instance] Spoker details.
     * @apiSuccess {integer}    spoker.id                       [instance] Spoker's identifier.
     * @apiSuccess {string}     spoker.name                     [instance] Spoker's name.
     * @apiSuccess {string}     spoker.picture                  [instance] Spoker profile's picture.
     * @apiSuccess {string}     spoker.gender                   [instance] Spoker's <a href="#lexicon-users-genders">gender</a>.
     * @apiSuccess {object}     from                            [instance] Previous spoker details.
     * @apiSuccess {integer}    from.id                         [instance] Previous spoker's identifier.
     * @apiSuccess {string}     from.name                       [instance] Previous spoker's name.
     * @apiSuccess {string}     from.picture                    [instance] Previous spoker profile's picture.
     * @apiSuccess {string}     from.gender                     [instance] Previous spoker's <a href="#lexicon-users-genders">gender</a>.
     * @apiSuccess {string}     visibility                      [instance] Spok's <a href="#lexicon-spoks-visibilities">visibility</a>.
     * @apiSuccess {object}     counters                        Original spok's counters (since its very first launch).
     * @apiSuccess {integer}    counter.nb_spoked               Number of re-spoke.
     * @apiSuccess {integer}    counter.nb_scoped               Number of users related to this spok (in the user's spoks stack or already re-spoked or un-spoked).
     * @apiSuccess {integer}    counter.nb_comments             Number of comments.
     * @apiSuccess {decimal}    counter.distance                Spok's total travelling distance (in meters).
     * 
     * @apiUse SpokContent
     * 
     * @apiUse SPK001
     * @apiUse SPK002
     * @apiUse SPK101
     */
    public function getOneShort() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Spok
     * @apiName view one (full)
     * @api {get} /spok/[id]/full view one (full)
     * @apiDescription Service to get one spok with full details.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam    {integer}   id  Spok identifier.
     * 
     * @apiSuccess {string}     type                    <a href="#lexicon-spoks-types">Spok's type</a>.
     * @apiSuccess {timestamp}  ttl                     Spok's <a href="#lexicon-spok-TTL">TTL</a>.
     * @apiSuccess {timestamp}  launched                Original spok's launching timestamp.
     * @apiSuccess {string}     text                    Original spok's text.
     * @apiSuccess {timestamp}  respoked                [instance] Current re-spoked instance's timestamp.
     * @apiSuccess {string}     curtext                 [instance] Current re-spoked instance's text.
     * @apiSuccess {object}     spoker                  [instance] Spoker details.
     * @apiSuccess {integer}    spoker.id               [instance] Spoker's identifier.
     * @apiSuccess {string}     spoker.name             [instance] Spoker's name.
     * @apiSuccess {string}     spoker.picture          [instance] Spoker profile's picture.
     * @apiSuccess {string}     spoker.gender           [instance] Spoker's <a href="#lexicon-users-genders">gender</a>.
     * @apiSuccess {object}     from                    [instance] Previous spoker details.
     * @apiSuccess {integer}    from.id                 [instance] Previous spoker's identifier.
     * @apiSuccess {string}     from.name               [instance] Previous spoker's name.
     * @apiSuccess {string}     from.picture            [instance] Previous spoker profile's picture.
     * @apiSuccess {string}     from.gender             [instance] Previous spoker's <a href="#lexicon-users-genders">gender</a>.
     * @apiSuccess {string}     visibility              [instance] Spok's <a href="#lexicon-spoks-visibilities">visibility</a>.
     * @apiSuccess {object}     counters                Original spok's counters (since its very first launch).
     * @apiSuccess {integer}    counter.nb_spoked       Number of re-spoke.
     * @apiSuccess {integer}    counter.nb_scoped       Number of users related to this spok (in the user's spoks stack or already re-spoked or un-spoked).
     * @apiSuccess {integer}    counter.nb_comments     Number of comments.
     * @apiSuccess {decimal}    counter.distance        Spok's total travelling distance (in meters).
     * @apiSuccess {object[]}   respokers               Last 10 re-spokers.
     * @apiSuccess {integer}    respoker.id             Re-spoker's identifier.
     * @apiSuccess {string}     respoker.name           Re-spoker's name.
     * @apiSuccess {string}     respoker.picture        Re-spoker profile's picture.
     * @apiSuccess {string}     respoker.gender         Re-spoker's <a href="#lexicon-users-genders">gender</a>.
     * @apiSuccess {object[]}   scoped                  Last 10 scoped users.
     * @apiSuccess {integer}    scoped.id               Scoped user's identifier.
     * @apiSuccess {string}     scoped.name             Scoped user's name.
     * @apiSuccess {string}     scoped.picture          Scoped user profile's picture.
     * @apiSuccess {string}     scoped.gender           Scoped user's <a href="#lexicon-users-genders">gender</a>.
     * @apiSuccess {object[]}   comments                Last 10 comments.
     * @apiSuccess {text}       comment.text            Comment's text.
     * @apiSuccess {timestamp}  comment.timestamp       Comment's timestamp.
     * @apiSuccess {integer}    comment.user.id         Commentator's identifier.
     * @apiSuccess {string}     comment.user.name       Commentator's name.
     * @apiSuccess {string}     comment.user.picture    Commentator profile's picture.
     * @apiSuccess {string}     comment.user.gender     Commentator's <a href="#lexicon-users-genders">gender</a>.
     * 
     * @apiUse SpokContent
     * 
     * @apiUse SPK001
     * @apiUse SPK002
     * @apiUse SPK101
     */
    public function getOneFull() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Spok
     * @apiName view one (re-spokers)
     * @api {get} /spok/[id]/respokers[/pos] view one (re-spokers)
     * @apiDescription Service to get 10 respokers of a spok.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam    {integer}   id      Spok identifier.
     * @apiParam    {string}    [pos]   Pagination position identifier.
     *
     * @apiSuccess {string}     previous                Pagination position identifier of the previous page of re-spokers.
     * @apiSuccess {string}     next                    Pagination position identifier of the next page of re-spokers.
     * @apiSuccess {object[]}   respokers               Last 10 re-spokers.
     * @apiSuccess {integer}    respoker.id             Re-spoker's identifier.
     * @apiSuccess {string}     respoker.name           Re-spoker's name.
     * @apiSuccess {string}     respoker.picture        Re-spoker profile's picture.
     * @apiSuccess {string}     respoker.gender         Re-spoker's <a href="#lexicon-users-genders">gender</a>.
     * 
     * @apiUse SPK001
     * @apiUse SPK002
     * @apiUse SPK102
     */
    public function getSpokeRespokers() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Spok
     * @apiName view one (scoped)
     * @api {get} /spok/[id]/scoped[/pos] view one (scoped users)
     * @apiDescription Service to get 10 scoped users of a spok.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam    {integer}   id      Spok identifier.
     * @apiParam    {string}    [pos]   Pagination position identifier.
     *
     * @apiSuccess {string}     previous                Pagination position identifier of the previous page of scoped users.
     * @apiSuccess {string}     next                    Pagination position identifier of the next page of scoped users.
     * @apiSuccess {object[]}   scoped                  Last 10 scoped users.
     * @apiSuccess {integer}    scoped.id               Scoped user's identifier.
     * @apiSuccess {string}     scoped.name             Scoped user's name.
     * @apiSuccess {string}     scoped.picture          Scoped user profile's picture.
     * @apiSuccess {string}     scoped.gender           Scoped user's <a href="#lexicon-users-genders">gender</a>.
     * 
     * @apiUse SPK001
     * @apiUse SPK002
     * @apiUse SPK103
     */
    public function getSpokScoped() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Spok
     * @apiName view one (comments)
     * @api {get} /spok/[id]/comments[/pos] view one (comments)
     * @apiDescription Service to get 10 comments of a spok.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam    {integer}   id      Spok identifier.
     * @apiParam    {string}    [pos]   Pagination position identifier.
     *
     * @apiSuccess {string}     previous                Pagination position identifier of the previous page of comments.
     * @apiSuccess {string}     next                    Pagination position identifier of the next page of comments.
     * @apiSuccess {object[]}   comments                Last 10 comments.
     * @apiSuccess {text}       comment.text            Comment's text.
     * @apiSuccess {timestamp}  comment.timestamp       Comment's timestamp.
     * @apiSuccess {integer}    comment.user.id         Commentator's identifier.
     * @apiSuccess {string}     comment.user.name       Commentator's name.
     * @apiSuccess {string}     comment.user.picture    Commentator profile's picture.
     * @apiSuccess {string}     comment.user.gender     Commentator's <a href="#lexicon-users-genders">gender</a>.
     * 
     * @apiUse SPK001
     * @apiUse SPK002
     * @apiUse SPK104
     */
    public function getSpokComments() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Spok
     * @apiName stack
     * @api {get} /spoks[/pos] stack
     * @apiDescription Service to load the spoks stack.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam    {string}    [pos]   Pagination position identifier.
     *
     * @apiSuccess {string}     previous    Pagination position identifier of the previous page of spoks in the stack.
     * @apiSuccess {string}     next        Pagination position identifier of the next page of spoks in the stack.
     * @apiSuccess {object[]}   spoks       Last 10 pending <a href="#api-Spok-view_one__short_">spoks</a>.
     * 
     * @apiUse SPK105
     */
    public function getStack() { return; }

    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Spok
     * @apiName disable
     * @api {websocket} / disable
     * @apiDescription Event emitted when a spok is disabled (by the spok's owner or an admin).
     * <br />Event received by all the spok's instance.
     * 
     * @apiUse AuthRequired
     * 
     * @apiExample {js} Emitted / received example
     * {
     *  spokid: 5
     * }
     * 
     * @apiUse SPK001
     * @apiUse SPK115
     */
    public function spokDisable() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Spok
     * @apiName remove
     * @api {websocket} / remove
     * @apiDescription Event emitted when a spok is removed by a user from its wall.
     * <br />Event received by all users interacting with the spok instance (wall, view).
     * 
     * @apiUse AuthRequired
     * 
     * @apiExample {js} Emitted / received example
     * {
     *  spokid: 5,
     *  userid: 981,
     *  instanceid: 654
     * }
     * 
     * @apiUse SPK001
     * @apiUse SPK116
     */
    public function spokRemove() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Spok
     * @apiName respoke
     * @api {websocket} /spok/[id]/respoke respoke
     * @apiDescription Event received by all the <a href="#lexicon-publication-targets">targeted users</a> and also by all the current users interacting with the spok (in their stacks, in the wall they are visiting, ...).
     * <br />A <a href="#api-My_Account-notification">"notification"</a> event is also sent.
     * <br />Add the user to the list of the <a href="#api-Spok-feed_subscribe___unsubscribe">spok's feed subscribers</a> (will be notified of spok's activity).
     * <br />Add a new "respoked" <a href="#api-My_Account-notifications">notifications</a> to all the subscribers of the spok's feed (by default: spok instance's owner, spok's creator and spok's commentators).
     * <br />If the spok instance's text mention a specific user, a new "mention" <a href="#api-My_Account-notifications">notification</a> is also added for the mentionned user and a corresponding <a href="#api-My_Account-notification">"notification"</a> is sent too.
     * <br />Event emitted when a spok is re-spoked:
     * - user's re-spoke action,
     * - <a href="#api-Spok-create_media__step_2___upload_">create media (step 2 - upload)</a>,
     * - <a href="#api-Spok-create_text__step_2___publication_">create text (step 2 - publication)</a>,
     * - <a href="#api-Spok-create_poll__step_4___publication_">create poll (step 4 - publication)</a>.
     * 
     * @apiUse AuthRequired
     * 
     * @apiUse EventSpokStructure
     * 
     * @apiParam    {integer}   id              Spok's identifier.
     * @apiParam    {integer}   groupid         Identifier of the user's group the spok to be posted.
     * @apiParam    {string}    [visibility]    Default "public". Set the <a href="#lexicon-spok-instances-visibilities">spok's visibility</a>.
     * @apiParam    {string}    [text]          Text for the spok instance.
     * 
     * @apiUse GeoCoordinates
     * 
     * @apiUse GRP001
     * @apiUse RGX006
     * @apiUse RGX009
     * @apiUse RGX010
     * @apiUse SPK001
     * @apiUse SPK006
     * @apiUse SPK009
     * @apiUse SPK117
     */
    public function spokRespoke() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Spok
     * @apiName unspoke
     * @api {websocket} /spok/[id]/unspoke unspoke
     * @apiDescription Event emitted when a <a href="#api-Spok-un_spoke">Spok is unspoked</a>.
     * <br />Event received by all the users currently interacting with the spok (in their stacks, in the wall they are visiting, ...).
     * 
     * @apiUse AuthRequired
     * 
     * @apiUse EventSpokStructure
     * 
     * @apiParam    {integer}   id  Spok's identifier.
     * 
     * @apiUse GeoCoordinates
     * 
     * @apiUse SPK001
     * @apiUse SPK006
     * @apiUse SPK118
     */
    public function spokUnspoke() { return; }

    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Spok
     * @apiName comment
     * @api {websocket} /spok/[id]/comment comment
     * @apiDescription Event emitted when a spok is commented.
     * <br />Event received by all the users currently interacting with the spok (in their stacks, in the wall they are visiting, ...).
     * <br />Send also a <a href="#api-My_Account-notification">"notification"</a> events.
     * <br />Add a new "comment" <a href="#api-My_Account-notifications">notifications</a> to all the subscribers of the spok's feed (by default: spok instance's owner, spok's creator and spok's commentators).
     * <br />If the comment mention a specific user, a new "mention" <a href="#api-My_Account-notifications">notification</a> is also added for the mentionned user and a corresponding <a href="#api-My_Account-notification">"notification"</a> is sent too.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam    {integer}   id      Spok's identifier.
     * @apiParam    {string}    text    Comment's text.
     * 
     * @apiUse GeoCoordinates
     * 
     * @apiExample {js} Received example
     * {
     *    spok: [see event spok structure tab],
     *    comment: {
     *      id: 95487,
     *      text: "some sample comment\nwith another line",
     *      author: [see event user structure tab]
     *    }
     * }
     * @apiUse EventSpokStructure
     * @apiUse EventUserStructure
     * 
     * @apiUse RGX009
     * @apiUse RGX010
     * @apiUse SPK001
     * @apiUse SPK119
     */
    public function comment() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Spok
     * @apiName comment (update)
     * @api {websocket} /comment/[comid] comment (update)
     * @apiDescription Event emitted when a spok's comment is updated (only by its author).
     * <br />Event received by all the users currently interacting with the spok (in their stacks, in the wall they are visiting, ...).
     * <br />If the comment mention a specific user, a new "mention" <a href="#api-My_Account-notifications">notification</a> is added for the mentionned user and a corresponding <a href="#api-My_Account-notification">"notification"</a> is sent too.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam    {integer}   comid   Comment's identifier.
     * @apiParam    {string}    text    Comment's text.
     * 
     * @apiUse GeoCoordinates
     * 
     * @apiExample {js} Emitted / received example
     * {
     *    spok: [see event spok structure tab],
     *    comment: {
     *      id: 95487,
     *      text: "some sample comment\nwith another line",
     *    }
     * }
     * @apiUse EventSpokStructure
     * 
     * @apiUse RGX009
     * @apiUse RGX010
     * @apiUse SPK008
     * @apiUse SPK120
     */
    public function commentUpdate() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Spok
     * @apiName comment (remove)
     * @api {websocket} /comment/[comid] comment (remove)
     * @apiDescription Event emitted when a spok's comment is removed (only by comment's author or spok's original creator).
     * <br />Event received by all the users currently interacting with the spok (in their stacks, in the wall they are visiting, ...).
     * 
     * @apiUse AuthRequired
     * 
     * @apiExample {js} Emitted / received example
     * {
     *    spok: [see event spok structure tab],
     *    comment: {
     *      id: 95487,
     *    }
     * }
     * @apiUse EventSpokStructure
     * 
     * @apiSuccess {object}     comment         Comment's details.
     * @apiSuccess {integer}    comment.id      Comment's identifier.
     * 
     * @apiUse SPK008
     * @apiUse SPK121
     */
    public function commentRemove() { return; }
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Spok
     * @apiName create
     * @api {websocket} /spoks create
     * @apiDescription Service to create a new spok.
     * <br />Send a <a href="#api-Spok-respoke">"respoke"</a> event.
     * <br />Add the user to the list of the <a href="#api-Spok-feed_subscribe___unsubscribe">spok's feed subscribers</a> (will be notified of spok's activity).
     * <br />If the spok instance's text mention a specific user, a new "mention" <a href="#api-My_Account-notifications">notification</a> is also added for the mentionned user and a corresponding <a href="#api-My_Account-notification">"notification"</a> is sent too.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam    {string}    content_type    Spok's <a href="#lexicon-spoks-types">type of content</a>.
     * @apiParam    {integer}   [groupid]       Default "0" is for followers. Identifier of the user's group the spok to be posted.
     * @apiParam    {string}    [visibility]    Default "public". Set the <a href="#lexicon-spok-instances-visibilities">spok's visibility</a>.
     * @apiParam    {integer}   [ttl]           Default "0" is for infinite. Spok's <a href="#lexicon-spok-TTL">TTL</a> (in seconds).
     * @apiParam    {string}    [instance_text] Text for the spok instance's creator.
     * 
     * @apiParam    {string}    file            File to be <a href="#lexicon-upload_files">uploaded</a>.<br /><b>Only required and considered for picture, animated gif, video or sound.</b>
     * @apiParam    {string}    text            <b>Only required and considered if content type is "text".</b>
     * @apiParam    {object}    url             URL details.<br /><b>Only required and considered if content type is "url".</b>
     * @apiParam    {string}    url.address     URL to be spokd.
     * @apiParam    {string}    url.title       URL's title.
     * @apiParam    {string}    url.text        URL's text.
     * @apiParam    {string}    url.preview     URL's <a href="#lexicon-url-preview">preview</a>.
     * @apiParam    {string}    [url.type]      Default "unknown". <a href="#lexicon-url-content-types">URL's content type</a>.
     * 
     * @apiParam    {object}    poll            Poll's details.<br /><b>Only required and considered if content type is "poll".</b>
     * @apiParam    {string}    poll.title      Poll's title.
     * @apiParam    {string}    [poll.desc]     Poll's description.
     * @apiParam    {object[]}  poll.questions  Poll's questions (1-20 questions/poll).
     * @apiParam    {string}    poll.q.text     Poll question's text.
     * @apiParam    {string}    [poll.q.type]   Poll question's content type. Default "text". Same as <a href="#lexicon-url-content-types">URL's content type</a>.
     * @apiParam    {string}    [poll.q.preview] Poll question's <a href="#lexicon-url-preview">preview</a>.
     * @apiParam    {integer}   poll.q.rank    Poll question's rank.
     * @apiParam    {object[]}  poll.q.answers  Poll question's answers (2-10 answers/question).
     * @apiParam    {string}    poll.q.a.text   Answer's text.
     * @apiParam    {string}    [poll.q.a.type]  Answer's content type. Default "text". Same as <a href="#lexicon-url-content-types">URL's content type</a>.
     * @apiParam    {string}    [poll.q.a.preview] Answer's <a href="#lexicon-url-preview">preview</a>.
     * @apiParam    {integer}   poll.q.a.rank   Answer's rank.
     * 
     * @apiParam    {object}    riddle          Riddle's details.<br /><b>Only required and considered if content type is "riddle".</b>
     * @apiParam    {string}    riddle.title    Riddle's title.
     * @apiParam    {object}    riddle.question Riddle question's details.
     * @apiParam    {string}    riddle.q.text   Riddle question's text.
     * @apiParam    {string}    [riddle.q.type] Riddle question's content type. Default "text". Same as <a href="#lexicon-url-content-types">URL's content type</a>.
     * @apiParam    {string}    [riddle.q.preview]  Riddle question's <a href="#lexicon-url-preview">preview</a>.
     * @apiParam    {object}    riddle.answer   Riddle answer's details.
     * @apiParam    {string}    riddle.a.text   Riddle answer's text.
     * @apiParam    {string}    [riddle.a.type] Riddle answer's content type. Default "text". Same as <a href="#lexicon-url-content-types">URL's content type</a>.
     * @apiParam    {string}    [riddle.a.preview]  Riddle answer's <a href="#lexicon-url-preview">preview</a>.
     * 
     * @apiUse GeoCoordinates
     * 
     * @apiSuccess  {integer}   spokid         Spok's identifier.
     * 
     * @apiUse GRP001
     * @apiUse RGX006
     * @apiUse RGX007
     * @apiUse RGX008
     * @apiUse RGX010
     * @apiUse RGX011
     * @apiUse RGX012
     * @apiUse RGX013
     * @apiUse RGX014
     * @apiUse RGX015
     * @apiUse SPK003
     * @apiUse SPK005
     * @apiUse SPK011
     * @apiUse SPK012
     * @apiUse SPK013
     * @apiUse SPK106
     * @apiUse SPK107
     * @apiUse SPK108
     * @apiUse SPK109
     */
    public function createSpokStep() { return; }

    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Spok
     * @apiName poll (view question)
     * @api {get} /poll/[questionid] poll (view question)
     * @apiDescription Service to view a poll spok's question.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam    {integer}   questionid  Question's identifier.
     * 
     * @apiSuccess  {object}    previous    Previous question's details.
     * @apiSuccess  {integer}   prev.id     Previous question's identifier.
     * @apiSuccess  {string}    prev.text   Previous question's text.
     * @apiSuccess  {object}    current     Current question's details.
     * @apiSuccess  {integer}   curr.id     Current question's identifier.
     * @apiSuccess  {string}    curr.text   Current question's text.
     * @apiSuccess  {object}    next        Next question's details.
     * @apiSuccess  {integer}   next.id     Next question's identifier.
     * @apiSuccess  {string}    next.text   Next question's text.
     * @apiSuccess  {object[]}  answers     Question's answers ordered by ascending rank.
     * @apiSuccess  {integer}   answ.id     Answer's identifier.
     * @apiSuccess  {integer}   answ.rank   Answer's rank.
     * @apiSuccess  {string}    answ.text   Answer's text.
     * 
     * @apiUse SPK001
     * @apiUse SPK004
     * @apiUse SPK112
     */
    public function viewPollQuestion() { return; }    
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Spok
     * @apiName poll (answer a question)
     * @api {websocket} /poll/[questionid]  poll (answer a question)
     * @apiDescription Service to answer to a poll spok's question.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam    {integer}   questionid  Question's identifier.
     * @apiParam    {integer}   answerid    Answer's identifier.
     * 
     * @apiUse GeoCoordinates
     * 
     * @apiUse SPK001
     * @apiUse SPK004
     * @apiUse SPK010
     * @apiUse SPK124
     */
    public function answerPollQuestion() { return; }    

    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Spok
     * @apiName comments
     * @api {get} /spok/[id]/comments[/pos] comments
     * @apiDescription Service to read the comments of a spok.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam {integer}  id      Spok identifier.
     * @apiParam {string}   [pos]   Pagination position identifier.
     *
     * @apiSuccess {string}     previous                Pagination position identifier of the previous page of comments.
     * @apiSuccess {string}     next                    Pagination position identifier of the next page of comments.
     * @apiSuccess {integer}    nb_comments             Number of comments.
     * @apiSuccess {object[]}   publication_comments    Comments list, in chronological order.
     * @apiSuccess {string}     comment.id              Comment identifier.
     * @apiSuccess {timestamp}  comment.date            Comment timestamp.
     * @apiSuccess {string}     comment.text            Comment text.
     * @apiSuccess {object}     comment.user            Comment user <a href="#api-Users-profile__minicard_">minicard details</a>.
     * 
     * @apiUse SPK001
     * @apiUse SPK122
     */
    public function getComments() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Spok
     * @apiName feed subscribe / unsubscribe
     * @api {websocket} /spok/[id]/subscribe feed subscribe / unsubscribe
     * @apiDescription Switch-service to subscribe/unsubscribe to a spok's feed (to start/stop receiving <a href="#api-My_Account-notifications">notifications</a> from this spok).
     * 
     * @apiUse AuthRequired
     * 
     * @apiUse SPK001
     * @apiUse SPK123
     */
    public function subscribeFeed() { return; }
}