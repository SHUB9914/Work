<?php

class Messages
{
    /**
     * @apiVersion 1.0.0
     * @apiGroup Messaging
     * @apiName talks
     * @api {get} /talks[/pos] talks
     * @apiDescription Service to get the list of talks (one friend <=> one talk).
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam {string}   [pos]   Pagination position identifier (1 page => 10 talks).
     * 
     * @apiSuccess {string}     previous            Pagination position identifier of the previous page of talks.
     * @apiSuccess {string}     next                Pagination position identifier of the next page of talks.
     * @apiSuccess {object[]}   talks               Talks list, from the most recent activity to the oldest.
     * @apiSuccess {integer}    talk.id             Talk identifier.
     * @apiSuccess {object}     talk.user           Talk's user details.
     * @apiSuccess {integer}    talk.user.id        Talk user's identifier.
     * @apiSuccess {string}     talk.user.nickname  Talk user's nickname.
     * @apiSuccess {string}     talk.user.picture   Talk user's picture.
     * @apiSuccess {string}     talk.user.gender    Talk user's <a href="#lexicon-users-genders">gender</a>.
     * @apiSuccess {object}     talk.last           Talk's last message's details.
     * @apiSuccess {timestamp}  talk.last.timestamp Talk last message's timestamp.
     * @apiSuccess {string}     talk.last.text      Talk last message's text.
     * 
     * @apiUse MSG101
     */
    public function getTalks() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Messaging
     * @apiName talk
     * @api {get} /talk/[id][/pos] talk
     * @apiDescription Service to get the messages from a talk.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam {string}   [pos]   Pagination position identifier (1 page => 20 messages).
     * 
     * @apiSuccess {string}     previous            Pagination position identifier of the previous page of messages.
     * @apiSuccess {string}     next                Pagination position identifier of the next page of messages.
     * @apiSuccess {object}     me                  My details.
     * @apiSuccess {integer}    me.id               My identifier.
     * @apiSuccess {string}     me.nickname         My nickname.
     * @apiSuccess {string}     me.picture          My picture.
     * @apiSuccess {string}     me.gender           My <a href="#lexicon-users-genders">gender</a>.
     * @apiSuccess {object}     user                Talk's user details.
     * @apiSuccess {integer}    user.id             Talk user's identifier.
     * @apiSuccess {string}     user.nickname       Talk user's nickname.
     * @apiSuccess {string}     user.picture        Talk user's picture.
     * @apiSuccess {string}     user.gender         Talk user's <a href="#lexicon-users-genders">gender</a>.
     * @apiSuccess {object[]}   messages            Messages list, from the most recent to the oldest.
     * @apiSuccess {integer}    message.id          Message's identifier.
     * @apiSuccess {string}     message.text        Message's text.
     * @apiSuccess {timestamp}  message.timestamp   Message's timestamp.
     * 
     * @apiUse MSG102
     */
    public function getTalk() { return; }

    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Messaging
     * @apiName remove talk
     * @api {websocket} /talk/[id] remove talk
     * @apiDescription Service to remove a talk (all messages).
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam {integer}  id      Talk's identifier.
     * 
     * @apiUse MSG002
     * @apiUse MSG104
     */
    public function removeTalk() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Messaging
     * @apiName remove message
     * @api {websocket} /message/[id] remove message
     * @apiDescription Service to remove a message.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam {integer}  id      Message's identifier.
     * 
     * @apiUse MSG003
     * @apiUse MSG105
     */
    public function removeMsg() { return; }

    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Messaging
     * @apiName send message
     * @api {websocket} /talk/[id] send message
     * @apiDescription Event emitted when a message is sent.
     * <br />Event received by message's sender and receiver.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam {integer}  id      Talk's identifier.
     * @apiParam {string}   text    Message's text to be sent.
     * 
     * @apiExample {js} Emitted / received example
     * {
     *  sender: [see event user structure tab],
     *  receiver: [see event user structure tab],
     *  message: {
     *      id: 11654,
     *      message: "a sample message\nwith multiple lines",
     *      file: "[binary-data]",
     *  }
     * }
     * @apiUse EventUserStructure
     * 
     * @apiSuccess {object}     sender      Message's sender (see user data structure below).
     * @apiSuccess {object}     receiver    Message's receiver (see user data structure below).
     * @apiSuccess {object}     message     Message's details.
     * @apiSuccess {integer}    id          Message's identifier.
     * @apiSuccess {string}     message     Message's text.
     * @apiSuccess {string}     file        Message's attached file (binary).
     * 
     * @apiUse MSG001
     * @apiUse MSG002
     * @apiUse MSG103
     */
    public function messageSend() { return; }
}