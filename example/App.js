import React from 'react';
import { View, Text } from 'react-native';
import {createBottomTabNavigator,createAppContainer} from 'react-navigation';
import AppMain from './appMain';

const AppNavigator = createBottomTabNavigator({
App1Screen: {
    screen: AppMain,
},

export default createAppContainer(AppNavigator);
