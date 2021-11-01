extern crate log;

use std::{
    io::{Read, Write},
    net::{TcpListener, TcpStream},
    sync::atomic::{AtomicBool, Ordering},
};

use crate::{game_state::GameState, utils::*};

const TAG: &str = "server_native";
const COMMAND_SHOW_ROLE: u8 = 0;
const COMMAND_CORRECT_MOVE: u8 = 1;
const COMMAND_INVALID_MOVE: u8 = 2;
const COMMAND_FINISH_GAME: u8 = 3;

pub(crate) struct Server {
    listener: TcpListener,
    is_game_ended: AtomicBool,
    current_stream: Option<TcpStream>,
}

impl Server {
    #[inline]
    pub(crate) fn new(ip: String) -> std::io::Result<Server> {
        log::debug!("{} new", TAG);

        Ok(Server {
            listener: TcpListener::bind(format!("{}:1337", ip))?,
            is_game_ended: AtomicBool::default(),
            current_stream: None,
        })
    }

    #[inline]
    pub(crate) fn get_current_stream(&self) -> &Option<TcpStream> {
        &self.current_stream
    }

    #[inline]
    pub(crate) fn get_current_stream_mut(&mut self) -> &mut Option<TcpStream> {
        &mut self.current_stream
    }

    #[inline]
    pub(crate) fn set_current_stream(&mut self, stream: TcpStream) {
        self.current_stream = Some(stream)
    }

    #[inline]
    pub(crate) fn get_listener(&self) -> &TcpListener {
        &self.listener
    }

    #[inline]
    pub(crate) fn get_listener_mut(&mut self) -> &mut TcpListener {
        &mut self.listener
    }

    #[inline]
    pub(crate) fn get_game_ended(&self) -> &AtomicBool {
        &self.is_game_ended
    }

    #[inline]
    pub(crate) fn get_game_ended_mut(&mut self) -> &mut AtomicBool {
        &mut self.is_game_ended
    }

    #[inline]
    pub(crate) fn set_game_ended_mut(&mut self, is_game_ended: bool) {
        self.is_game_ended.store(is_game_ended, Ordering::Relaxed)
    }

    #[inline]
    pub(crate) fn read_move(stream: &mut TcpStream) -> TcpIOResult<(u8, u8)> {
        log::debug!("{} read_move", TAG);

        let mut data = [0; 2];
        handle_err_value(stream.read_exact(&mut data), unsafe {
            (*data.get_unchecked(0), *data.get_unchecked(1))
        })
    }

    #[inline]
    pub(crate) fn send_role(stream: &mut TcpStream, client_player_role: u8) {
        log::debug!("{} send_role", TAG);
        log_err_if_exists(stream.write(&[COMMAND_SHOW_ROLE, client_player_role]))
    }

    #[inline]
    pub(crate) fn send_correct_move(stream: &mut TcpStream, table: [[u8; 3]; 3]) {
        log::debug!("{} send_correct_move", TAG);

        let mut f = vec![COMMAND_CORRECT_MOVE];
        f.extend(table.iter().flat_map(|x| *x));
        log_err_if_exists(stream.write(f.as_slice()))
    }

    #[inline]
    pub(crate) fn send_invalid_move(stream: &mut TcpStream) {
        log::debug!("{} send_invalid_move", TAG);
        log_err_if_exists(stream.write(&[COMMAND_INVALID_MOVE]))
    }

    #[inline]
    pub(crate) fn send_game_finished(stream: &mut TcpStream, state: GameState) {
        log::debug!("{} send_game_finished", TAG);
        log_err_if_exists(stream.write(&[COMMAND_FINISH_GAME, state as u8]))
    }
}
