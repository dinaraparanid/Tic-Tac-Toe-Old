extern crate log;

use std::{
    io,
    io::{Read, Write},
    net::TcpStream,
};

use crate::utils::*;

const TAG: &str = "client_player";
pub(crate) const PLAYER_IS_FOUND: u8 = 0;
pub(crate) const PLAYER_MOVED: u8 = 1;

pub(crate) struct ClientPlayer {
    stream: TcpStream,
}

impl ClientPlayer {
    #[inline]
    pub(crate) fn new(ip: String) -> io::Result<ClientPlayer> {
        log::debug!("{} new", TAG);

        Ok(ClientPlayer {
            stream: TcpStream::connect(format!("{}:1337", ip))?,
        })
    }

    #[inline]
    pub(crate) fn send_ready(&mut self) {
        log::debug!("{} send_ready", TAG);
        log_err_if_exists(self.stream.write(&[PLAYER_IS_FOUND]));
        log::debug!("{} sent ready", TAG);
    }

    #[inline]
    pub(crate) fn send_move(&mut self, y: u8, x: u8) {
        log::debug!("{} send_move", TAG);
        log_err_if_exists(self.stream.write(&[PLAYER_MOVED, y, x]));
        log::debug!("{} sent move", TAG);
    }

    #[inline]
    fn read_byte(&mut self) -> TcpIOResult<u8> {
        let mut data = [0];
        handle_err_value(self.stream.read_exact(&mut data), unsafe {
            *data.get_unchecked(0)
        })
    }

    #[inline]
    pub(crate) fn read_command(&mut self) -> TcpIOResult<u8> {
        log::debug!("{} read_command", TAG);
        self.read_byte()
    }

    #[inline]
    pub(crate) fn read_role(&mut self) -> TcpIOResult<u8> {
        log::debug!("{} read_role", TAG);
        self.read_byte()
    }

    #[inline]
    pub(crate) fn read_state(&mut self) -> TcpIOResult<u8> {
        log::debug!("{} read_state", TAG);
        self.read_byte()
    }

    #[inline]
    pub(crate) fn read_table(&mut self) -> TcpIOResult<[[u8; 3]; 3]> {
        log::debug!("{} read_table", TAG);

        let mut data = [0; 9];
        handle_err_callback(self.stream.read_exact(&mut data), || {
            let mut table = [[0; 3]; 3];
            let mut iter = data.iter();

            (0..3).for_each(|i| {
                (0..3).for_each(|q| unsafe {
                    *table.get_unchecked_mut(i).get_unchecked_mut(q) = *iter.next().unwrap()
                })
            });

            table
        })
    }
}
