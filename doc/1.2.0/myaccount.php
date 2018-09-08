<?php

class MyAccount
{
    /**
     * @apiVersion 1.0.0
     * @apiGroup My Account
     * @apiName set profile
     * @api {websocket} /my/profile set profile
     * @apiDescription Service to change my profile details.
     * <br />A <a href="#api-My_Account-user_updated">"user updated"</a> event is sent.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam {string}   nickname    User's <a href="#lexicon-nickname-format">nickname</a>.
     * @apiParam {date}     birthdate   User's <a href="#lexicon-users-ages">birthdate</a>.
     * @apiParam {string}   gender      User's <a href="#lexicon-users-genders">gender</a>.
     * @apiParam {string}   picture     User's picture file to be uploaded.
     * @apiParam {string}   cover       User's cover picture file to be uploaded.
     * 
     * @apiUse GeoCoordinates
     * 
     * @apiUse RGX003
     * @apiUse RGX004
     * @apiUse RGX005
     * @apiUse TIME008
     * @apiUse PIC001
     * @apiUse PIC002
     * @apiUse PIC003
     * @apiUse PIC101
     * @apiUse MYA101
     */
    public function setMyProfile() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup My Account
     * @apiName notifications
     * @api {get} /my/notifications[/pos] notifications
     * @apiDescription Service to view a wall's <a href="#lexicon-notifications-types">notifications</a>.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam {string}   [pos]   Pagination position identifier (1 page => 10 notifications).
     *
     * @apiSuccess {string}     previous                Pagination position identifier of the previous page of notifications.
     * @apiSuccess {string}     next                    Pagination position identifier of the next page of notifications.
     * @apiSuccess {object[]}   notifications           Notifications list, from the most recent to the oldest.
     * @apiSuccess {string}     notif.id                Notification's identifier.
     * @apiSuccess {string}     notif.type              Notification's <a href="#lexicon-notifications-types">type</a>.
     * @apiSuccess {integer}    notif.related_to        Notification's object identifier related to (depends on type, often a spok instance).
     * @apiSuccess {timestamp}  notif.timestamp         Notification's timestamp.
     * @apiSuccess {object}     notif.emitter           Notification's emitter details.
     * @apiSuccess {integer}    notif.emitter.id        Notification emitter's identifier.
     * @apiSuccess {string}     notif.emitter.nickname  Notification emitter's nickname.
     * @apiSuccess {string}     notif.emitter.picture   Notification emitter's picture.
     * @apiSuccess {string}     notif.emitter.gender    Notification emitter's <a href="#lexicon-users-genders">gender</a>.
     * 
     * @apiUse MYA102
     */
    public function getNotifications() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup My Account
     * @apiName notification (remove)
     * @api {websocket} /my/notifications/[id] notification (remove)
     * @apiDescription Service to view a wall's <a href="#lexicon-notifications-types">notifications</a>.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam {integer}  id  Notification's identifier.
     *
     * @apiUse MYA001
     * @apiUse MYA103
     */
    public function removeNotification() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup My Account
     * @apiName help
     * @api {websocket} /my/settings/help help
     * @apiDescription Switch-service to enable/disable the help.
     * 
     * @apiUse AuthRequired
     *
     * @apiUse MYA104
     */
    public function setHelp() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup My Account
     * @apiName follows
     * @api {websocket} /my/settings/follows follows
     * @apiDescription Switch-service for followers and following lists.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam    {boolean}   followers   Enable / disable public visibility of the list of followers.
     * @apiParam    {boolean}   following   Enable / disable public visibility of the list of following.
     *
     * @apiUse MYA105
     */
    public function setFollowsList() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup My Account
     * @apiName user updated
     * @api {websocket} / user updated
     * @apiDescription Event emitted when <a href="#api-My_Account-set_profile">an user updates its profile</a> (picture, nickname, ...).
     * <br />Event received by all the users viewing or interacting with something related to the updated user (spok, comment, message).
     * 
     * @apiUse AuthRequired
     * 
     * @apiUse EventUserStructure
     */
    public function userUpdated() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup My Account
     * @apiName notification
     * @api {websocket} / notification
     * @apiDescription Event emitted each time a <a href="#api-My_Account-notifications">notification</a> is added.
     * <br />Event received by related notification's user.
     * 
     * @apiUse AuthRequired
     * 
     * @apiExample {js} Emitted / received example
     * {
     *   userid: 67954,
     *   notification: {
     *     id: 9135,
     *     type: "comment",
     *     related_to: 2784,
     *     emitter: [see event user structure tab]
     *   }
     * }
     * 
     * @apiUse EventUserStructure
     * 
     * @apiSuccess {integer}    userid              User's identifier to be notified.
     * @apiSuccess {object}     notification        Notification's details.
     * @apiSuccess {integer}    notif.id            Notification's identifier.
     * @apiSuccess {string}     notif.type          Notification's <a href="#lexicon-notifications-types">type</a>.
     * @apiSuccess {integer}    notif.related_to    Notification's object identifier related to (depends on type, often a spok instance).
     * @apiSuccess {object}     emitter             User causing the notification's emission (see user data structure below).
     */
    public function notificationAdded() { return; }
}
