<?php

/**
 * @apiDefine AuthRequired
 * @apiHeader {string}  Authorization   Bearer [token].
 * @apiError SYST-401 [HTTP 401] Authentication required.
 */

/**
 * @apiDefine AdminAuthRequired
 * @apiHeader {string}  AdminAuthorization  Bearer [token].
 * @apiError SYST-401 [HTTP 401] Authentication required.
 */

/**
 * @apiDefine AuthBearerToken
 * @apiHeader {string}  Authorization   Bearer [token].
 */

/**
 * @apiDefine EventSpokStructure
 * @apiExample {js} Event spok structure
 * {
 *  id: 5
 *  counters: {
 *      nb_respoked: 621,
 *      nb_landed: 1734,
 *      nb_comments: 157,
 *      travelled: 1645378
 *  }
 * }
 * @apiSuccess {object}     spok                        Spok's details.<br />[related to the event spok structure tab just above]
 * @apiSuccess {integer}    spok.id                     Spok's identifier.
 * @apiSuccess {object}     spok.counters               Spok's statistics.
 * @apiSuccess {integer}    spok.counter.nb_respoked    Spok's number of re-spoked.
 * @apiSuccess {integer}    spok.counter.nb_landed      Spok's number of user related.
 * @apiSuccess {integer}    spok.counter.nb_comment     Spok's number of comments.
 * @apiSuccess {integer}    spok.counter.travelled      Spok's total travelled distance (based on re-spoking users geo coordinates).
 */

/**
 * @apiDefine EventUserStructure
 * @apiExample {js} Event user structure
 * {
 *  id: 6845,
 *  nickname: "John Doe",
 *  gender: "male",
 *  picture: "https://static.spok.me/users/6845.jpg",
 * }
 * @apiSuccess {object}     user            User's details.<br />[related to the event user structure tab just above]
 * @apiSuccess {integer}    user.id         User's identifier.
 * @apiSuccess {string}     user.nickname   User's nickname.
 * @apiSuccess {string}     user.gender     User's <a href="#lexicon-users-genders">gender</a>.
 * @apiSuccess {string}     user.picture    User's profile picture URL.
 */

/**
 * @apiDefine MiniCardProfile
 * @apiSuccess {integer}    id          User's identifier.
 * @apiSuccess {string}     nickname    User's nickname.
 * @apiSuccess {string}     gender      User's <a href="#lexicon-users-genders">gender</a>.
 * @apiSuccess {string}     picture     User's profile picture URL.
*/

/**
 * @apiDefine CardProfile
 * @apiSuccess {integer}    id              User's identifier.
 * @apiSuccess {string}     nickname        User's nickname.
 * @apiSuccess {string}     gender          User's <a href="#lexicon-users-genders">gender</a>.
 * @apiSuccess {string}     picture         User's profile picture URL.
 * @apiSuccess {string}     cover           User's cover picture URL.
 * @apiSuccess {integer}    nb_followers    User's number of followers.
 * @apiSuccess {integer}    nb_following    User's number of following.
 * @apiSuccess {integer}    nb_spoks        User's number of spoks.
*/

/**
 * @apiDefine SpokContent
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
*/

/**
 * @apiDefine GeoCoordinates
 * @apiParam    {object}  geo           Geocoordinates
 * @apiParam    {decimal} geo.latitude  Latitude coordinate
 * @apiParam    {decimal} geo.longitude Longitude coordinate
 * @apiParam    {decimal} geo.elevation Elevation coordinate
 * 
 * @apiError GEO-001 Invalid latitude.
 * @apiError GEO-002 Invalid longitude.
 * @apiError GEO-003 Invalid elevation.
 * @apiError GEO-101 Invalid geocoordinates (generic error).
 */

