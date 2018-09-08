<?php

class Search
{
    /**
     * @apiVersion 1.0.0
     * @apiGroup Search
     * @apiName popular spoks
     * @api {get} /search/popular[/pos] popular spoks
     * @apiDescription Service to get most popular spokers.
     * 
     * @apiUse AuthRequired
     * 
     * @apiSuccess {string}     previous            Pagination position identifier of the previous page of popular spokers.
     * @apiSuccess {string}     next                Pagination position identifier of the next page of popular spokers.
     * @apiSuccess {object[]}   spokers             Most 10 popular spokers.
     * @apiSuccess {integer}    spoker.id           Spoker's identifier.
     * @apiSuccess {string}     spoker.nickname     Spoker's nickname.
     * @apiSuccess {string}     spoker.gender       Spoker's <a href="#lexicon-users-genders">gender</a>.
     * @apiSuccess {string}     spoker.picture      Spoker's profile picture URL.
     * @apiSuccess {integer}    spoker.nb_followers Spoker's number of followers.
     * @apiSuccess {integer}    spoker.nb_following Spoker's number of following.
     * @apiSuccess {integer}    spoker.nb_spoks     Spoker's number of spoks.
     * 
     * @apiUse SRH101
     */
    public function getPopularSpokers() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Search
     * @apiName trendy spoks
     * @api {get} /search/trendy[/pos] trendy spoks
     * @apiDescription Service to get most recent trendy spoks.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam    {string}    [pos]   Pagination position identifier.
     *
     * @apiSuccess {string}     previous    Pagination position identifier of the previous page of trendy spoks.
     * @apiSuccess {string}     next        Pagination position identifier of the next page of trendy spoks.
     * @apiSuccess {object[]}   spoks       Most 10 trendy <a href="#api-Spok-view_one__short_">spoks</a>.
     * 
     * @apiUse SRH102
     */
    public function getTrendySpoks() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Search
     * @apiName friends spoks
     * @api {get} /search/friends[/pos] friends spoks
     * @apiDescription Service to get the last spoks of my friends.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam    {string}    [pos]   Pagination position identifier.
     *
     * @apiSuccess {string}     previous    Pagination position identifier of the previous page of my friends spoks.
     * @apiSuccess {string}     next        Pagination position identifier of the next page of my friends spoks.
     * @apiSuccess {object[]}   spoks       Last 10 <a href="#api-Spok-view_one__short_">spoks</a> fo my friends.
     * 
     * @apiUse SRH103
     */
    public function getFriendsSpoks() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Search
     * @apiName last spoks
     * @api {get} /search/last[/pos] last spoks
     * @apiDescription Service to get the last spoks.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam    {string}    [pos]   Pagination position identifier.
     *
     * @apiSuccess {string}     previous    Pagination position identifier of the previous page of the last spoks.
     * @apiSuccess {string}     next        Pagination position identifier of the next page of the last spoks.
     * @apiSuccess {object[]}   spoks       Last 10 <a href="#api-Spok-view_one__short_">spoks</a>.
     * 
     * @apiUse SRH104
     */
    public function getLastSpoks() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Search
     * @apiName launch search
     * @api {get} /search/?[pos][nicknames][hashtags][locations][start][end] launch search
     * @apiDescription Service to spok's full search.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam    {string}    [pos]                   Pagination position identifier.
     * @apiParam    {string[]}  [nicknames]             Searched spoker's nickname. UI/UX note: autocompleted with the <a href="#api-Search-autocomplete_nickname">autocomplete nickname</a> webservice, starts with "@", to be removed before sending to API.
     * @apiParam    {string[]}  [hashtags]              Searched hashtag. UI/UX note: autocompleted with the <a href="#api-Search-autocomplete_hashtag">autocomplete hashtag</a> webservice, starts with "#", to be removed before sending to API.
     * @apiParam    {object[]}  [locations]             Searched locations geo-coordinates. UI/UX note: autocompleted with the <a href="#api-Search-autocomplete_location">autocomplete location</a> webservice.
     * @apiParam    {decimal}   [location.latitude]     Searched location's latitude.
     * @apiParam    {decimal}   [location.longitude]    Searched location's longitude.
     * @apiParam    {timestamp} [start]                 Searched start's period.
     * @apiParam    {timestamp} [end]                   Searched end's period.
     *
     * @apiSuccess {string}     previous    Pagination position identifier of the previous page of the last spoks.
     * @apiSuccess {string}     next        Pagination position identifier of the next page of the last spoks.
     * @apiSuccess {object[]}   spoks       Retrieved 10 <a href="#api-Spok-view_one__short_">spoks</a>, ordered from the most recent to the oldest.
     * 
     * @apiUse SRH001
     * @apiUse SRH002
     * @apiUse TIME010
     * @apiUse SRH105
     */
    public function searchSpoks() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Search
     * @apiName autocomplete nickname
     * @api {get} /search/autonick/?[nickname] autocomplete nickname
     * @apiDescription Service to autocomplete nickname.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam    {string}    nickname    Nickname part to be completed.
     *
     * @apiSuccess  {string[]}  nicknames   Most relevant 10 nicknames.
     * 
     * @apiUse SRH003
     * @apiUse SRH106
     */
    public function autocompleteNickname() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Search
     * @apiName autocomplete hashtag
     * @api {get} /search/autohash/?[hashtag] autocomplete hashtag
     * @apiDescription Service to autocomplete hashtag.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam    {string}    hashtag     Hashtag part to be completed.
     *
     * @apiSuccess  {string[]}  hashtags    Most relevant 10 hashtags.
     * 
     * @apiUse SRH004
     * @apiUse SRH107
     */
    public function autocompleteHashtag() { return; }
    
    
    /**
     * @apiVersion 1.0.0
     * @apiGroup Search
     * @apiName autocomplete location
     * @api {get} /search/autoloc/?[location] autocomplete location
     * @apiDescription Service to autocomplete location.
     * 
     * @apiUse AuthRequired
     * 
     * @apiParam    {string[]}  location    Location part to be completed.
     *
     * @apiSuccess  {object[]}  googlemap   Array of retrieved locations from <a href="#lexicon-googlemap-address">Google Map</a> service.
     * 
     * @apiUse SRH005
     * @apiUse SRH108
     */
    public function autocompleteLocation() { return; }
}