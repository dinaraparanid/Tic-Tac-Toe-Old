use std::{
    io::{Read, Write},
    net::{Shutdown, TcpListener, TcpStream},
    sync::atomic::AtomicBool,
};

use crate::{
    client_player::{PLAYER_IS_FOUND, PLAYER_MOVED},
    utils::*,
};

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
    pub(crate) fn read_move(stream: &mut TcpStream) -> StackTraceValueResult<(u8, u8)> {
        let mut data = [0; 2];
        handle_err_value(stream.read_exact(&mut data), unsafe {
            (*data.get_unchecked(0), *data.get_unchecked(1))
        })
    }

    #[inline]
    pub(crate) fn run_bfsm(&mut self) {
        for stream in self.listener.incoming() {
            if { *self.is_game_ended.get_mut() } {
                break;
            }

            if let Ok(mut stream) = stream {
                let mut data = [0];

                while match stream.read(&mut data) {
                    Ok(size) => match size {
                        0 => false,

                        _ => {
                            let command = unsafe { *data.get_unchecked(0) };

                            match command {
                                PLAYER_IS_FOUND => {}

                                PLAYER_MOVED => {}

                                _ => unreachable!(),
                            }

                            true
                        }
                    },

                    Err(_) => unsafe {
                        stream.shutdown(Shutdown::Both).unwrap_unchecked();
                        false
                    },
                } {}
            }
        }
    }

    #[inline]
    pub(crate) fn send_role(stream: &mut TcpStream, client_player_role: u8) -> StackTraceResult {
        get_err_if_exists(stream.write(&[COMMAND_SHOW_ROLE, client_player_role]))
    }

    #[inline]
    pub(crate) fn send_correct_move(
        stream: &mut TcpStream,
        table: [[u8; 3]; 3],
    ) -> StackTraceResult {
        get_err_if_exists(unsafe {
            stream.write(&[
                COMMAND_CORRECT_MOVE,
                *table.get_unchecked(0).get_unchecked(0),
                *table.get_unchecked(0).get_unchecked(1),
                *table.get_unchecked(0).get_unchecked(2),
                *table.get_unchecked(1).get_unchecked(0),
                *table.get_unchecked(1).get_unchecked(1),
                *table.get_unchecked(1).get_unchecked(2),
                *table.get_unchecked(2).get_unchecked(0),
                *table.get_unchecked(2).get_unchecked(1),
                *table.get_unchecked(2).get_unchecked(2),
            ])
        })
    }

    #[inline]
    pub(crate) fn send_invalid_move(stream: &mut TcpStream) -> StackTraceResult {
        get_err_if_exists(stream.write(&[COMMAND_INVALID_MOVE]))
    }

    #[inline]
    pub(crate) fn send_game_finished(stream: &mut TcpStream) -> StackTraceResult {
        get_err_if_exists(stream.write(&[COMMAND_FINISH_GAME]))
    }
}
