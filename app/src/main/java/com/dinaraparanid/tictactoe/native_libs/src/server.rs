use std::{
    io::{Read, Write},
    net::{Shutdown, TcpListener, TcpStream},
    sync::atomic::AtomicBool,
};

const COMMAND_SHOW_ROLE: u8 = 0;
const COMMAND_CORRECT_MOVE: u8 = 1;
const COMMAND_INVALID_MOVE: u8 = 2;
const COMMAND_GAME_FINISHED: u8 = 3;

pub(crate) struct Server {
    listener: TcpListener,
    is_game_ended: AtomicBool,
}

impl Server {
    #[inline]
    pub(crate) fn new(ip: String) -> std::io::Result<Server> {
        Ok(Server {
            listener: TcpListener::bind(format!("{}:1337", ip))?,
            is_game_ended: AtomicBool::default(),
        })
    }

    #[inline]
    pub(crate) fn read_move(stream: &mut TcpStream) -> (u8, u8) {
        let mut data = [0; 2];
        unsafe {
            stream.read_exact(&mut data).unwrap_unchecked();
            (*data.get_unchecked(0), *data.get_unchecked(1))
        }
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
                        _ => true,
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
    pub(crate) fn send_roles(stream: &mut TcpStream, client_player_role: u8) {
        unsafe {
            stream
                .write(&[COMMAND_SHOW_ROLE, client_player_role])
                .unwrap_unchecked()
        };
    }

    #[inline]
    pub(crate) fn send_correct_move(stream: &mut TcpStream, table: [[u8; 3]; 3]) {
        unsafe {
            stream
                .write(&[
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
                .unwrap_unchecked()
        };
    }

    #[inline]
    pub(crate) fn send_invalid_move(stream: &mut TcpStream) {
        unsafe { stream.write(&[COMMAND_INVALID_MOVE]).unwrap_unchecked() };
    }

    #[inline]
    pub(crate) fn send_game_finished(stream: &mut TcpStream) {
        unsafe { stream.write(&[COMMAND_GAME_FINISHED]).unwrap_unchecked() };
    }
}
