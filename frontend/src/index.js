import React from 'react';
import anychart from 'anychart';
import ReactDOM from 'react-dom/client';
import './index.scss';
import App from './App';
import {anychartKey} from "./private/apiKeys";
import getCurrentMode from "./data/const/modes";

anychart.licenseKey(anychartKey);

document.title = getCurrentMode().header

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);